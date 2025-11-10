package dev.lixqa.egyptiancasino.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class BlackjackGame implements CasinoGame {

    @Override
    public String getId() {
        return "blackjack";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("bj");
    }

    @Override
    public String getDescription() {
        return "Auto-play a hand of blackjack against the dealer.";
    }

    @Override
    public void play(GameContext context) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        List<Integer> playerCards = new ArrayList<>();
        List<Integer> dealerCards = new ArrayList<>();

        playerCards.add(drawCard(random));
        playerCards.add(drawCard(random));
        dealerCards.add(drawCard(random));
        dealerCards.add(drawCard(random));

        while (handValue(playerCards) < 17) {
            playerCards.add(drawCard(random));
        }
        while (handValue(dealerCards) < 17) {
            dealerCards.add(drawCard(random));
        }

        int playerScore = bestScore(playerCards);
        int dealerScore = bestScore(dealerCards);

        Component summary = Component.text("You: " + playerCards + " (" + playerScore + ") | Dealer: " + dealerCards + " (" + dealerScore + ")", NamedTextColor.AQUA);
        context.sendMessage(summary);

        boolean playerBust = playerScore > 21;
        boolean dealerBust = dealerScore > 21;

        if (playerBust && dealerBust) {
            context.refundBet();
            return;
        }

        if (playerBust) {
            context.lose();
            return;
        }

        if (dealerBust || playerScore > dealerScore) {
            context.winWithMultiplier(2.0);
        } else if (playerScore == dealerScore) {
            context.refundBet();
        } else {
            context.lose();
        }
    }

    private int drawCard(ThreadLocalRandom random) {
        return random.nextInt(1, 12); // 1-11
    }

    private int handValue(List<Integer> cards) {
        return cards.stream().mapToInt(Integer::intValue).sum();
    }

    private int bestScore(List<Integer> cards) {
        int total = 0;
        int aces = 0;
        for (int card : cards) {
            if (card == 1) {
                aces++;
                total += 11;
            } else {
                total += Math.min(card, 10);
            }
        }
        while (total > 21 && aces > 0) {
            total -= 10;
            aces--;
        }
        return total;
    }
}
