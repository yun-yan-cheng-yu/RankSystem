package com.zqyyz.ranksystem.rocksdb.user;

/**
 * 用户表（RocksDB key: user:<playerId>）的数据模型。
 * 目前只有注册时间和登录时间；未来加字段直接在 record 里加即可，
 * 旧数据反序列化时缺失字段会取默认值，向后兼容。
 */
public record UserInfo(
        String playerId,
        long registerTimeMillis,
        long loginTimeMillis
) {
}