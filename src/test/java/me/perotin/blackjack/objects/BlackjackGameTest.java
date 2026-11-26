package me.perotin.blackjack.objects;

import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;
import static me.perotin.blackjack.objects.BlackjackGame.Ending.*;

public class BlackjackGameTest {
    private final BlackjackGame game = new BlackjackGame(null, 100);

    @Test public void scoringPreservesOrderAndHandlesMultipleAces() {
        List<String> hand = Arrays.asList("As", "10h", "Ac");
        assertEquals(12, game.getScoreUnder21(hand));
        assertEquals(Arrays.asList("As", "10h", "Ac"), hand);
        assertEquals(21, game.getScoreUnder21(Arrays.asList("As", "Ah", "9d")));
    }

    @Test public void dealerKeepsDrawingUntilSeventeen() {
        game.getHouseCards().clear();
        game.getHouseCards().addAll(Arrays.asList("2s", "3c"));
        game.setCardsAvailible(new ArrayList<>(Arrays.asList("3h", "3s", "3d", "3c")));
        game.playDealer();
        assertEquals(17, game.getScoreUnder21(game.getHouseCards()));
        assertEquals(6, game.getHouseCards().size());
    }

    @Test public void threeCardTwentyOneSettlesOnceAtOrdinaryOdds() {
        game.getPlayerCards().clear();
        game.getPlayerCards().addAll(Arrays.asList("7c", "7d", "7s"));
        RoundResult result = game.finish(WIN, 1.5, 10, 50, true);
        assertEquals(190, result.payout, 0);
        assertEquals(90, result.net, 0);
        assertEquals(10, result.tax, 0);
        assertFalse(result.blackjack);
        assertNull(game.finish(LOSE, 1.5, 0, 50, true));
        assertSame(result, game.getSettlement());
    }

    @Test public void naturalPaysTaxOnTheBonus() {
        game.getPlayerCards().clear();
        game.getPlayerCards().addAll(Arrays.asList("As", "Ks"));
        RoundResult result = game.finish(WIN, 1.5, 10, 50, true);
        assertTrue(result.blackjack);
        assertEquals(235, result.payout, 0);
        assertEquals(135, result.net, 0);
        assertEquals(15, result.tax, 0);
    }

    @Test public void surrenderReportsAmountActuallyLost() {
        RoundResult result = game.finish(SURRENDER, 1.5, 0, 25, true);
        assertEquals(75, result.payout, 0);
        assertEquals(-25, result.net, 0);
    }

    @Test public void xpReportsWholeLevelsCredited() {
        RoundResult result = new RoundResult(SURRENDER, 5, false, 1.5, 0, 50, false);
        assertEquals(2, result.payout, 0);
        assertEquals(-3, result.net, 0);
    }

    @Test public void tieReturnsTheWholeStake() {
        assertEquals(100, game.finish(TIE, 1.5, 0, 50, true).payout, 0);
    }
}
