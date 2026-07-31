package com.jonathansandell.expense_splitter.balance;

import com.jonathansandell.expense_splitter.entity.Expense;
import com.jonathansandell.expense_splitter.entity.Group;
import com.jonathansandell.expense_splitter.entity.Member;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Plain JUnit test, no Spring context at all - proves BalanceCalculator has
 * no framework dependency and can be tested in complete isolation.
 */
class BalanceCalculatorTest {

    private final BalanceCalculator calculator = new BalanceCalculator();

    private final Group group = new Group("Cabin Trip");
    private final Member alice = new Member("Alice", group);
    private final Member bob = new Member("Bob", group);
    private final Member carol = new Member("Carol", group);

    @Test
    void evenSplitBetweenTwoMembers() {
        Expense expense = new Expense("Groceries", new BigDecimal("50.00"), group, alice, Set.of(alice, bob));

        List<MemberBalance> balances = calculator.calculate(List.of(expense));

        assertThat(balanceOf(balances, alice)).isEqualByComparingTo("25.00");
        assertThat(balanceOf(balances, bob)).isEqualByComparingTo("-25.00");
    }

    @Test
    void unevenSplitDistributesRemainderCentsInParticipantOrder() {
        Set<Member> participants = new LinkedHashSet<>(List.of(alice, bob, carol));
        Expense expense = new Expense("Dinner", new BigDecimal("10.00"), group, alice, participants);

        List<MemberBalance> balances = calculator.calculate(List.of(expense));

        // $10.00 / 3 = $3.33 each with 1 cent left over; the first
        // participant in iteration order (alice) absorbs the extra cent.
        assertThat(balanceOf(balances, alice)).isEqualByComparingTo("6.66");
        assertThat(balanceOf(balances, bob)).isEqualByComparingTo("-3.33");
        assertThat(balanceOf(balances, carol)).isEqualByComparingTo("-3.33");
    }

    @Test
    void balancesAlwaysSumToZero() {
        Set<Member> allThree = new LinkedHashSet<>(List.of(alice, bob, carol));
        List<Expense> expenses = List.of(
                new Expense("Dinner", new BigDecimal("10.00"), group, alice, allThree),
                new Expense("Gas", new BigDecimal("37.41"), group, bob, allThree),
                new Expense("Snacks", new BigDecimal("8.02"), group, carol, Set.of(carol, alice))
        );

        List<MemberBalance> balances = calculator.calculate(expenses);

        BigDecimal sum = balances.stream()
                .map(MemberBalance::netBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void payerWhoIsTheOnlyParticipantNetsToZero() {
        Expense expense = new Expense("Personal snack", new BigDecimal("4.50"), group, alice, Set.of(alice));

        List<MemberBalance> balances = calculator.calculate(List.of(expense));

        assertThat(balanceOf(balances, alice)).isEqualByComparingTo("0.00");
    }

    @Test
    void multipleExpensesAccumulateOntoTheSameMember() {
        List<Expense> expenses = List.of(
                new Expense("Groceries", new BigDecimal("50.00"), group, alice, Set.of(alice, bob)),
                new Expense("Movie tickets", new BigDecimal("30.00"), group, bob, Set.of(alice, bob))
        );

        List<MemberBalance> balances = calculator.calculate(expenses);

        // Alice: +25 (groceries) - 15 (movie) = +10
        // Bob:   -25 (groceries) + 15 (movie) = -10
        assertThat(balanceOf(balances, alice)).isEqualByComparingTo("10.00");
        assertThat(balanceOf(balances, bob)).isEqualByComparingTo("-10.00");
    }

    private static BigDecimal balanceOf(List<MemberBalance> balances, Member member) {
        return balances.stream()
                .filter(b -> b.member() == member)
                .findFirst()
                .orElseThrow()
                .netBalance();
    }
}
