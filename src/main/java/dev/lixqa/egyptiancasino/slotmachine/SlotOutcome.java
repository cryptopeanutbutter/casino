package dev.lixqa.egyptiancasino.slotmachine;

import java.util.List;

public record SlotOutcome(List<Integer> finalSymbols, int matchCount) {
}
