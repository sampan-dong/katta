#Katta

---

## Katta 是什么?

Katta 是一个分布式搜索引擎解决方案. 他和 Solr/ElasticSearch 工作模式不同, 它不进行创建索引, 只管理索引. 

---

## 部署

详细文档见 : https://gitee.com/yiidata/katta/blob/master/manual/doc/%E5%AE%89%E8%A3%85.md

## katta 目前支持 Hive 和 Presto

Hive 已经能完美支持，Presto 目前还并不完善，尤其是索引下推的部分，目前尚未掌握 Presto 内部机制，还在学习中，目前已经支持简单的索引下推 x=y（未来考虑全部支持）。

部分使用文档见: [使用文档](https://gitee.com/yiidata/katta/tree/master/manual/doc)

## 写在后面的话：

Katta 源项目见 [Katta](http://katta.sourceforge.net/), 该项目从 2011 年停止维护，但是我从 2013 年关注到这个项目，细读了内部的源码，对源码的结构和质量所折服。于是从13年到到这麽多年间不间断的维护， 并加入了非常多的特性和升级了很多依赖。并把原项目改变为一个 maven 模块化的项目，修复了诸多的 bug 和增强了内部的功能。该工具在我前公司“识代运筹”的 dmp 标签库和速识2.1 升级版中使用过，稳定并且实现了很多看似不能实现的功能。

Katta 是一个灵感来源于 Hive 和 HBase 运行机制的大数据工具。

1. 索引的导入机制类似 Hive 对表数据的管理；
2. 基于 Index 和 Shard 机制又非常类似于 Hbase 对 Table 和 Region 的管理；
3. 内部 ZooKeeper 的选举机制，事件机制（Master-Slave）都是 Hbase 的灵魂；

怀抱利器，郁郁适兹土。吾知其必有合也。

思前想后，在这个2018年的元旦日我把代码开放出来。愿 Katta 能社区一起同进步，愿更多囿于大数据搜索的团队能一起完善。

这么几年，怀抱这个大数据的葵花宝典孤芳自赏，始终拿不起放不下。为什么是“葵花抱歉”不是其它，不是北冥神功？因为在大数据领域，你要入库的高性能，又要享受众多条件的快速查询，你必须付出巨大的代价，就必须舍去一些东西。这东西，就是实时更新。众所周知，所有的数据库（大数据）都有入库效率和查询效率的掣肘。Hive 能管理几十亿的数据集，但是并不能提供实时的搜索，如果在百亿千亿的数据集中找到你想要的几条，犹如大海捞针，难度不用说。Hbase 也同样能托管数百亿的数据，但是能查询的列只有一个。虽然社区有 hbase 的二级索引方案？谁用谁就知道那有多大的坑。Katta 才是超大数据集搜索的解决方案。

## 慎重选型：

并不是 Katta 比 Solr/ES好，在大部分使用场景下，Katta 没有 Solr、ES 易用。但是Solr 和 ES 定位便是做一个易用，全面的搜索引擎。它适应的是通用型索引和搜索的场景，而没有考虑真正大数据的情况下的痛点。即便是他们现在也支持大数据的接口，但是那只是事后的一种折中的解决办法。Katta 我认为定应该定位为超大数据集提供搜索的解决办法。这个超大数据集的单位为：亿，我甚至认为 Katta 能提供百亿，千亿级的搜索，而且使用的集群资源并不会太大。但是这对于 Solr、ES 来说，我认为不可能。

Katta 不支持适时更新，他只是为大规模数据集提供搜索的解决方案。Katta 管理着 Lucene 的 Index，但是并不自己生成 Index。 Katta-hadoop 中提供了 LuceneDocumentOutputFormat 的Hadoop API，可以使用 HadoopMapReduce 或者 Spark 来生成索引。然后通过命令把生成的分布式索引集合通过 addIndex 命令导入到 Katta。

最后，感谢原作者（原作者好像是印度人）提供伟大的思路，促使我多年的念念不忘，如今还是让他归于社区，开放给所有对大数据（真正的大数据）有搜索痛点的团队。

下载：[百度云盘](https://pan.baidu.com/s/1eSD70DO)
资料：[部分使用资料](https://gitee.com/yiidata/katta/tree/master/manual/doc)


