package com.jonathansandell.expense_splitter.balance;

import com.jonathansandell.expense_splitter.entity.Group;
import com.jonathansandell.expense_splitter.entity.Member;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain JUnit test, no Spring context - proves SettlementCalculator has no
 * framework dependency and can be tested in complete isolation.
 */
class SettlementCalculatorTest {

    private final SettlementCalculator calculator = new SettlementCalculator();

    private final Group group = new Group("Cabin Trip");
    private final Member alice = new Member("Alice", group);
    private final Member bob = new Member("Bob", group);
    private final Member carol = new Member("Carol", group);
    private final Member dave = new Member("Dave", group);

    @Test
    void twoMembersProduceOneTransfer() {
        List<MemberBalance> balances = List.of(
                new MemberBalance(alice, new BigDecimal("25.00")),
                new MemberBalance(bob, new BigDecimal("-25.00"))
        );

        List<Transfer> transfers = calculator.calculate(balances);

        assertThat(transfers).containsExactly(new Transfer(bob, alice, new BigDecimal("25.00")));
    }

    @Test
    void alreadySettledBalancesProduceNoTransfers() {
        List<MemberBalance> balances = List.of(
                new MemberBalance(alice, BigDecimal.ZERO),
                new MemberBalance(bob, BigDecimal.ZERO)
        );

        List<Transfer> transfers = calculator.calculate(balances);

        assertThat(transfers).isEmpty();
    }

    @Test
    void largestCreditorAndDebtorAreMatchedFirst() {
        // Largest creditor (alice, +30) is matched against largest debtor
        // (carol, -20) first, fully settling carol in one transfer. The
        // leftover +10 on alice is then settled against bob in a second
        // transfer, rather than alice ending up with two small payments.
        List<MemberBalance> balances = List.of(
                new MemberBalance(alice, new BigDecimal("30.00")),
                new MemberBalance(bob, new BigDecimal("-10.00")),
                new MemberBalance(carol, new BigDecimal("-20.00"))
        );

        List<Transfer> transfers = calculator.calculate(balances);

        assertThat(transfers).containsExactly(
                new Transfer(carol, alice, new BigDecimal("20.00")),
                new Transfer(bob, alice, new BigDecimal("10.00"))
        );
    }

    @Test
    void zeroBalanceMemberIsExcludedFromTransfers() {
        List<MemberBalance> balances = List.of(
                new MemberBalance(alice, new BigDecimal("25.00")),
                new MemberBalance(bob, new BigDecimal("-25.00")),
                new MemberBalance(carol, BigDecimal.ZERO)
        );

        List<Transfer> transfers = calculator.calculate(balances);

        assertThat(transfers).noneMatch(t -> t.from() == carol || t.to() == carol);
    }

    @Test
    void unevenCentsAreFullyAccountedFor() {
        // Mirrors BalanceCalculatorTest's uneven-split remainder scenario:
        // $10.00 split 3 ways leaves alice owed 6.66 and bob/carol owing
        // 3.33 each. Every cent alice is owed must show up across transfers.
        List<MemberBalance> balances = List.of(
                new MemberBalance(alice, new BigDecimal("6.66")),
                new MemberBalance(bob, new BigDecimal("-3.33")),
                new MemberBalance(carol, new BigDecimal("-3.33"))
        );

        List<Transfer> transfers = calculator.calculate(balances);

        BigDecimal totalToAlice = transfers.stream()
                .filter(t -> t.to() == alice)
                .map(Transfer::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(totalToAlice).isEqualByComparingTo("6.66");
    }

    @Test
    void transfersNetOutToTheOriginalBalancesForFourMembers() {
        List<MemberBalance> balances = List.of(
                new MemberBalance(alice, new BigDecimal("50.00")),
                new MemberBalance(bob, new BigDecimal("-20.00")),
                new MemberBalance(carol, new BigDecimal("-10.00")),
                new MemberBalance(dave, new BigDecimal("-20.00"))
        );

        List<Transfer> transfers = calculator.calculate(balances);

        Map<Member, BigDecimal> netEffect = netEffectOf(transfers);

        assertThat(netEffect.getOrDefault(alice, BigDecimal.ZERO)).isEqualByComparingTo("50.00");
        assertThat(netEffect.getOrDefault(bob, BigDecimal.ZERO)).isEqualByComparingTo("-20.00");
        assertThat(netEffect.getOrDefault(carol, BigDecimal.ZERO)).isEqualByComparingTo("-10.00");
        assertThat(netEffect.getOrDefault(dave, BigDecimal.ZERO)).isEqualByComparingTo("-20.00");
    }

    private static Map<Member, BigDecimal> netEffectOf(List<Transfer> transfers) {
        Map<Member, BigDecimal> net = new java.util.HashMap<>();
        for (Transfer transfer : transfers) {
            net.merge(transfer.to(), transfer.amount(), BigDecimal::add);
            net.merge(transfer.from(), transfer.amount().negate(), BigDecimal::add);
        }
        return net;
    }
}
