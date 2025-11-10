package dev.lixqa.egyptiancasino.game;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class DiceGame implements CasinoGame {

    @Override
    public String getId() {
        return "dice";
    }

    @Override
    public Set<String> getAliases() {
        return Set.of("die");
    }

    @Override
    public String getDescription() {
        return "Guess the roll of a six-sided die for a big payout.";
    }

    @Override
    public void play(GameContext context) {
        String[] args = context.getArgs();
        if (args.length == 0) {
            context.sendMessage(Component.text("Usage: /egyptiancasino dice <bet> <1-6>", NamedTextColor.YELLOW));
            context.refundBet();
            return;
        }

        int guess;
        try {
            guess = Integer.parseInt(args[0]);
        } catch (NumberFormatException exception) {
            context.sendMessage(Component.text("Your guess must be a number between 1 and 6.", NamedTextColor.RED));
            context.refundBet();
            return;
        }

        if (guess < 1 || guess > 6) {
            context.sendMessage(Component.text("Your guess must be between 1 and 6.", NamedTextColor.RED));
            context.refundBet();
            return;
        }

        int roll = ThreadLocalRandom.current().nextInt(1, 7);
        context.sendMessage(Component.text("The die rolls " + roll + "!", NamedTextColor.AQUA));

        if (roll == guess) {
            context.winWithMultiplier(6.0);
        } else {
            context.lose();
        }
    }
}
