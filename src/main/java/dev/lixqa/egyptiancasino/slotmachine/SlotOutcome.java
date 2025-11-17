package dev.lixqa.egyptiancasino.slotmachine;

import java.util.HashSet;
import java.util.List;

public record SlotOutcome(List<SlotSymbol> finalSymbols, int matchCount) {

    public boolean isJackpotWin() {
        if (matchCount != 3 || finalSymbols.isEmpty()) {
            return false;
        }
        SlotSymbol first = finalSymbols.get(0);
        if (!first.jackpotSymbol()) {
            return false;
        }
        return new HashSet<>(finalSymbols).size() == 1;
    }
}
