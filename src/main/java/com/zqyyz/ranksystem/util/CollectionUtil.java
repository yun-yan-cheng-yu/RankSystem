package com.zqyyz.ranksystem.util;

import java.util.List;

/**
 * 集合通用工具方法。
 */
public final class CollectionUtil {
    private CollectionUtil() {
    }

    /**
     * 按字典序比较两个整数列表。
     * <p>
     * 比较规则：先比较第一个元素；如果相等，再比较第二个元素；依次类推。
     * 如果公共部分完全相等，则更长的列表更大。
     * <p>
     * 返回值遵循 {@link Comparable#compareTo(Object)} 约定：
     * 负数表示 left 更小，0 表示相等，正数表示 left 更大。
     */
    public static int compareLexicographically(List<Integer> left, List<Integer> right) {
        int size = Math.min(left.size(), right.size());
        for (int index = 0; index < size; index++) {
            int result = Integer.compare(left.get(index), right.get(index));
            if (result != 0) {
                return result;
            }
        }
        return Integer.compare(left.size(), right.size());
    }
}
