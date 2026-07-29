package com.zqyyz.ranksystem.model;

import com.zqyyz.ranksystem.util.CollectionUtil;

import java.util.List;

public record HandValue(HandCategory category, List<Integer> tieBreakers, List<String> cards) implements Comparable<HandValue> {
    public HandValue {
        tieBreakers = List.copyOf(tieBreakers);
        cards = List.copyOf(cards);
    }

    public String name() {
        return category.displayName();
    }

    @Override
    public int compareTo(HandValue other) {
        // 先比较牌型，再按从大到小比较关键点数或踢脚牌。
        int categoryCompare = Integer.compare(category.strength(), other.category.strength());
        if (categoryCompare != 0) {
            return categoryCompare;
        }

        return CollectionUtil.compareLexicographically(tieBreakers, other.tieBreakers);
    }
}
