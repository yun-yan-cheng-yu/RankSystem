package com.zqyyz.ranksystem.rocksdb;

import org.rocksdb.CompressionType;
import org.rocksdb.FlushOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.WriteOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目内唯一的 RocksDB 实例持有者：负责打开/关闭数据库，并提供最底层的
 * get/put/delete、前缀遍历、属性统计和强制刷盘等操作。
 *
 * <p>RocksDB 的用法是原生的程序化配置：直接通过 {@link Options} 打开数据库
 * （目录默认 {@code data/rocksdb}，可用 {@code rocksdb.path} 覆盖），
 * 业务数据用可读 key 前缀区分（如 {@code score:}、{@code hand:}）。</p>
 *
 * <p>RocksDB 实例是线程安全的，所有业务存储（积分、结算历史）都复用这一个实例。
 * 每次写入使用 {@link WriteOptions#setSync(boolean)} 同步刷盘，保证积分写入后
 * 进程退出或重启不丢失（对 Demo 项目的写入量而言开销可接受）。</p>
 */
@Component
public class RocksDBStore {

    private static final Logger LOG = LoggerFactory.getLogger(RocksDBStore.class);

    private final Path dbPath;
    private final RocksDB db;
    private final WriteOptions writeOptions;

    public RocksDBStore(@Value("${rocksdb.path:data/rocksdb}") String path) {
        this.dbPath = Path.of(path).toAbsolutePath().normalize();
        try {
            Path parent = dbPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new IllegalStateException("failed to create RocksDB parent directory: " + dbPath.getParent(), e);
        }

        try {
            Options options = new Options()
                    .setCreateIfMissing(true)
                    .setCompressionType(CompressionType.LZ4_COMPRESSION);
            try {
                this.db = RocksDB.open(options, dbPath.toString());
            } finally {
                // 打开完成后 Options 不再需要，释放其 native 句柄（DB 内部持有自己的配置副本）。
                options.close();
            }
            this.writeOptions = new WriteOptions().setSync(true);
            LOG.info("RocksDB opened at {}", dbPath);
        } catch (RocksDBException e) {
            throw new IllegalStateException("failed to open RocksDB at " + dbPath, e);
        }
    }

    /** 数据库文件所在目录的绝对路径。 */
    public Path dbPath() {
        return dbPath;
    }

    /** 数据库是否仍持有底层 native 句柄（关闭后返回 false）。 */
    public boolean isOpen() {
        return db.isOwningHandle();
    }

    /** 读取一个 key，不存在时返回 null。 */
    public byte[] get(byte[] key) {
        try {
            return db.get(key);
        } catch (RocksDBException e) {
            throw new IllegalStateException("rocksdb get failed", e);
        }
    }

    /** 写入一个 key-value（覆盖已有值）。 */
    public void put(byte[] key, byte[] value) {
        try {
            db.put(writeOptions, key, value);
        } catch (RocksDBException e) {
            throw new IllegalStateException("rocksdb put failed", e);
        }
    }

    /** 删除一个 key（不存在时静默成功）。 */
    public void delete(byte[] key) {
        try {
            db.delete(writeOptions, key);
        } catch (RocksDBException e) {
            throw new IllegalStateException("rocksdb delete failed", e);
        }
    }

    /**
     * 按 key 字典序遍历所有以 {@code prefix} 开头的 key。
     * 返回的每个 byte[] 都是独立副本，迭代器移动后依然有效。
     */
    public List<byte[]> keysWithPrefix(byte[] prefix) {
        List<byte[]> keys = new ArrayList<>();
        try (RocksIterator iterator = db.newIterator()) {
            iterator.seek(prefix);
            while (iterator.isValid()) {
                byte[] key = iterator.key();
                if (!startsWith(key, prefix)) {
                    break;
                }
                keys.add(key);
                iterator.next();
            }
        }
        return keys;
    }

    /** 遍历统计数据库中的 key 总数（小数据量 Demo，直接全量迭代）。 */
    public long countKeys() {
        long count = 0;
        try (RocksIterator iterator = db.newIterator()) {
            iterator.seekToFirst();
            while (iterator.isValid()) {
                count++;
                iterator.next();
            }
        }
        return count;
    }

    /** 读取一个 RocksDB 内置属性（如 "rocksdb.stats"、"rocksdb.estimate-num-keys"）。 */
    public String property(String name) {
        try {
            String value = db.getProperty(name);
            return value == null ? "" : value;
        } catch (RocksDBException e) {
            throw new IllegalStateException("rocksdb property failed: " + name, e);
        }
    }

    /** 将内存中的写入强制刷到磁盘（等刷新完成）。 */
    public void flush() {
        try (FlushOptions flushOptions = new FlushOptions().setWaitForFlush(true)) {
            db.flush(flushOptions);
        } catch (RocksDBException e) {
            throw new IllegalStateException("rocksdb flush failed", e);
        }
    }

    /** 应用关闭时由 Spring 调用，释放数据库资源。 */
    @PreDestroy
    public void close() {
        if (writeOptions.isOwningHandle()) {
            writeOptions.close();
        }
        if (db.isOwningHandle()) {
            db.close();
        }
        LOG.info("RocksDB closed at {}", dbPath);
    }

    private static boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (value[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
