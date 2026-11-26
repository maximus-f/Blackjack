package me.perotin.blackjack.objects;

public final class RoundResult {
    public final BlackjackGame.Ending outcome;
    public final double stake;
    public final double payout;
    public final double net;
    public final double tax;
    public final boolean blackjack;

    RoundResult(BlackjackGame.Ending outcome, double stake, boolean natural, double multiplier,
                double taxPercent, double surrenderPercent, boolean cash) {
        this.outcome = outcome;
        this.stake = stake;
        blackjack = outcome == BlackjackGame.Ending.WIN && natural && multiplier > 1;
        double profit = stake * (blackjack ? multiplier : 1);
        tax = outcome == BlackjackGame.Ending.WIN ? profit * (taxPercent / 100) : 0;
        double credit;
        switch (outcome) {
            case WIN: credit = stake + (profit - tax); break;
            case TIE: credit = stake; break;
            case SURRENDER: credit = stake * (1 - surrenderPercent / 100); break;
            case LOSE: credit = 0; break;
            default: throw new AssertionError(outcome);
        }
        payout = cash ? credit : Math.floor(credit);
        net = payout - stake;
    }
}
