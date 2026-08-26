package com.zqyyz.ranksystem.rocksdb.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqyyz.ranksystem.rocksdb.RocksDBStore;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class RocksDBUserStore {

    private static final String USER_PREFIX_TEXT = "user:";

    private final RocksDBStore store;
    private final ObjectMapper json = new ObjectMapper();

    public RocksDBUserStore(RocksDBStore store) {
        this.store = store;
    }

    /** 读取用户信息，不存在返回 null。 */
    public UserInfo loadUser(String playerId) {
        byte[] value = store.get(userKey(playerId));
        if (value == null) {
            return null;
        }
        return fromBytes(value);
    }

    /** 写入 / 覆盖用户信息。 */
    public void saveUser(UserInfo user) {
        store.put(userKey(user.playerId()), toBytes(user));
    }

    private static byte[] userKey(String playerId) {
        return (USER_PREFIX_TEXT + playerId).getBytes(StandardCharsets.UTF_8);
    }

    private byte[] toBytes(UserInfo user) {
        try {
            return json.writeValueAsBytes(user);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("failed to serialize user", e);
        }
    }

    private UserInfo fromBytes(byte[] value) {
        try {
            return json.readValue(value, UserInfo.class);
        } catch (IOException e) {
            throw new IllegalStateException("failed to deserialize user", e);
        }
    }
}