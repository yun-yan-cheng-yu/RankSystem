package com.zqyyz.ranksystem.controller;

import com.zqyyz.ranksystem.rocksdb.user.UserInfo;
import com.zqyyz.ranksystem.rocksdb.RocksDBStore;
import com.zqyyz.ranksystem.rocksdb.user.RocksDBUserStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RocksDB 系统管理接口：查看数据库状态、统计信息、key 列表、玩家积分、
 * 结算历史，以及手动刷盘。Demo 用途，暂不做登录鉴权。
 */
@RestController
@RequestMapping("/rocksdb")
public class RocksDBController {

    private final RocksDBStore rocksDBStore;
    private final RocksDBUserStore userStore;

    public RocksDBController(RocksDBStore rocksDBStore, RocksDBUserStore userStore) {
        this.rocksDBStore = rocksDBStore;
        this.userStore = userStore;
    }

    // GET /rocksdb/status -> 数据库开关状态、路径、key 数和结算记录数
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("open", rocksDBStore.isOpen());
        result.put("path", rocksDBStore.dbPath().toString());
        result.put("keyCount", rocksDBStore.countKeys());
        return ResponseEntity.ok(result);
    }

    // GET /rocksdb/stats -> RocksDB 内置统计信息
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> stats() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("estimateNumKeys", rocksDBStore.property("rocksdb.estimate-num-keys"));
        result.put("stats", rocksDBStore.property("rocksdb.stats"));
        return ResponseEntity.ok(result);
    }

    // GET /rocksdb/keys?limit=500 -> 数据库中的 key 列表（按字典序）
    @GetMapping("/keys")
    public ResponseEntity<Map<String, Object>> keys(
            @RequestParam(value = "limit", defaultValue = "500") int limit) {
        List<String> keys = new ArrayList<>();
        for (byte[] key : rocksDBStore.keysWithPrefix(new byte[0])) {
            keys.add(new String(key, StandardCharsets.UTF_8));
            if (keys.size() >= limit) {
                break;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("count", keys.size());
        result.put("keys", keys);
        return ResponseEntity.ok(result);
    }

    // GET /rocksdb/score?id=player -> 某个玩家的持久化积分
    @GetMapping("/score")
    public ResponseEntity<Map<String, Object>> score(@RequestParam("id") String playerId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", playerId);
        return ResponseEntity.ok(result);
    }

    // POST /rocksdb/flush -> 手动把内存中的写入强制刷到磁盘
    @PostMapping("/flush")
    public ResponseEntity<Map<String, Object>> flush() {
        rocksDBStore.flush();
        return ResponseEntity.ok(Map.of("success", true));
    }

    // GET /rocksdb/user?id=player -> 查询用户表（注册时间 / 登录时间）
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> user(@RequestParam("id") String playerId) {
        UserInfo user = userStore.loadUser(playerId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", playerId);
        if (user != null) {
            result.put("registerTime", user.registerTimeMillis());
            result.put("loginTime", user.loginTimeMillis());
        }
        return ResponseEntity.ok(result);
    }
}
