package com.ivyft.katta.operation.master;

import com.ivyft.katta.lib.writer.ShardRange;
import com.ivyft.katta.master.MasterContext;
import com.ivyft.katta.operation.OperationId;
import com.ivyft.katta.operation.node.DeployResult;
import com.ivyft.katta.operation.node.NodeIndexMergeOperation;
import com.ivyft.katta.operation.node.OperationResult;
import com.ivyft.katta.protocol.CommitShards;
import com.ivyft.katta.protocol.InteractionProtocol;
import com.ivyft.katta.protocol.metadata.IndexDeployError;
import com.ivyft.katta.protocol.metadata.IndexMetaData;
import com.ivyft.katta.protocol.metadata.Shard;
import com.ivyft.katta.util.HadoopUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * <pre>
 *
 * Created by zhenqin.
 * User: zhenqin
 * Date: 16/3/25
 * Time: 10:39
 * Verdor: NowledgeData
 * To change this template use File | Settings | File Templates.
 *
 * </pre>
 *
 * @author zhenqin
 */
public class IndexMergeOperation extends AbstractIndexOperation {


    /**
     * 序列化表格
     */
    private static final long serialVersionUID = 1L;


    /**
     * 把结果写进 Zookeeper 中
     */
    protected final boolean createZkCommitId;


    /**
     * 当前 Commit 的 ID
     */
    protected final String commitId;

    /**
     * Commit Shards
     */
    protected final CommitShards commitShards;

    /**
     * Log
     */
    private static final Logger LOG = LoggerFactory.getLogger(IndexMergeOperation.class);


    /**
     * 构造方法
     *
     *
     * @param commitId Commit Id, 每次 Commit 唯一
     * @param commitShards Commit Shards
     * @param createZkCommitId 完成后是否把信息写入到 Zookeeper 中
     *
     */
    public IndexMergeOperation(String commitId, CommitShards commitShards, boolean createZkCommitId) {
        this.createZkCommitId = createZkCommitId;
        if(StringUtils.isBlank(commitId)) {
            this.commitId = commitShards.getCommitId();
        } else {
            this.commitId = commitId;
        }

        this.commitShards = commitShards;
    }



    @Override
    public ExecutionInstruction getExecutionInstruction(List<MasterOperation> runningOperations) throws Exception {
        for (MasterOperation operation : runningOperations) {
            if (operation instanceof IndexMergeOperation &&
                    ((IndexMergeOperation) operation).getIndexName().equals(this.getIndexName())) {
                return ExecutionInstruction.CANCEL;
            }
        }

        return ExecutionInstruction.EXECUTE;
    }

    @Override
    public List<OperationId> execute(MasterContext context, List<MasterOperation> runningOperations) throws Exception {
        InteractionProtocol protocol = context.getProtocol();
        List<String> liveNodes = protocol.getLiveNodes();
        LOG.info("liveNodes: " + liveNodes);

        //创建分布式的索引计划
        if (liveNodes.isEmpty()) {
            throw new IndexDeployException(IndexDeployError.ErrorType.NO_NODES_AVAILIBLE, "no nodes availible");
        }

        List<String> indices = protocol.getIndices();
        LOG.info("indices: " + indices);

        /*if(!indices.contains(commitShards.getIndexName())) {
            throw new IllegalStateException("katta can't contains " + commitShards.getIndexName() + " index.");
        }*/

        String commitId = commitShards.getCommitId();
        Set<ShardRange> commits = commitShards.getCommits();


        IndexMetaData indexMD = protocol.getIndexMD(commitShards.getIndexName());

        if (indexMD == null) {
            throw new IllegalStateException("index " + commitShards.getIndexName() + " can't found.");
        }

        LOG.info(indexMD.toString());
        // can be removed already
        if (!indexMD.hasDeployError()) {
            Set<Shard> shards = indexMD.getShards();

            //这一步检验每个Shard被几个Node所加载
            Map<String, List<String>> currentIndexShard2NodesMap = protocol
                    .getShard2NodesMap(Shard.getShardNames(shards));
            //{shard1=[node1, node2], shard2=[node2, node3]}

            Map<String, Set<String>> nodeInstalledShards = new HashMap<String, Set<String>>(3);
            for (Map.Entry<String, List<String>> entry : currentIndexShard2NodesMap.entrySet()) {
                List<String> nodeList = entry.getValue();
                for (String s : nodeList) {
                    Set<String> shardSet = nodeInstalledShards.get(s);
                    if(shardSet == null) {
                        shardSet = new HashSet<String>();
                        shardSet.add(entry.getKey());

                        nodeInstalledShards.put(s, shardSet);
                    } else {
                        shardSet.add(entry.getKey());
                    }
                }
            }

            LOG.info("index {} deploy nodes {}", commitShards.getIndexName(), nodeInstalledShards.toString());

            List<OperationId> operationIds = new ArrayList<OperationId>(nodeInstalledShards.size());
            for (String node : nodeInstalledShards.keySet()) {
                NodeIndexMergeOperation mergeOperation =
                        new NodeIndexMergeOperation(
                                node,
                                commitShards.getIndexName(),
                                commitId,
                                commits,
                                nodeInstalledShards.get(node)
                                );
                OperationId operationId = protocol.addNodeOperation(node, mergeOperation);
                operationIds.add(operationId);
            }

            return operationIds;
        }

        LOG.warn(ReflectionToStringBuilder.toString(indexMD));
        return null;
//        */

    }

    @Override
    public void nodeOperationsComplete(MasterContext context, List<OperationResult> results) throws Exception {
        LOG.info("index {} merge Complete.", this.getIndexName());

        Exception exception = null;

        for (OperationResult result : results) {
            LOG.info("node {} merge result {}", result.getNodeName(), result);

            if(result.getUnhandledException() != null) {
                LOG.error(ExceptionUtils.getFullStackTrace(result.getUnhandledException()));
                exception = result.getUnhandledException();
            }
        }

        Set<ShardRange> commits = getCommits();
        for (ShardRange commit : commits) {
            LOG.warn("delete index {} commitId {} path {}", getIndexName(), getCommitId(), commit.getShardPath());
            try {
                HadoopUtil.getFileSystem().delete(new Path(commit.getShardPath()), true);
            } catch (Exception e) {
                LOG.warn("", e);
            }
        }

        if(createZkCommitId) {
            Map<String, Map<String, Map<String, Object>>> nodeCommitMeta = new HashMap<String, Map<String, Map<String, Object>>>();
            //没有异常，则提交成功
            if (exception == null) {
                for (OperationResult result : results) {
                    if (result instanceof DeployResult) {
                        DeployResult r = (DeployResult) result;
                        Map<String, Map<String, String>> shardMetaDataMaps = r.getShardMetaDataMaps();
                        String nodeName = r.getNodeName();

                        nodeCommitMeta.put(nodeName, (Map) shardMetaDataMaps);
                    }
                }
            }

            try {
                context.getProtocol().getNewCommit(commitId, commitShards, nodeCommitMeta);
            } catch (Exception e) {
                LOG.warn("", e);
            }
        }


    }


    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + Integer.toHexString(hashCode()) + ":" + this.getIndexName();
    }

    private boolean isRecoverable(IndexDeployError deployError, int nodeCount) {
        if (deployError.getErrorType() == IndexDeployError.ErrorType.NO_NODES_AVAILIBLE && nodeCount > 0) {
            return true;
        }
        return false;
    }

    public CommitShards getCommitShards() {
        return commitShards;
    }


    public String getCommitId() {
        return commitShards.getCommitId();
    }

    public Set<ShardRange> getCommits() {
        return commitShards.getCommits();
    }

    public String getIndexName() {
        return commitShards.getIndexName();
    }


    public boolean isCreateZkCommitId() {
        return createZkCommitId;
    }
}
