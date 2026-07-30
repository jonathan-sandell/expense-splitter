package com.jonathansandell.expense_splitter.repository;

import com.jonathansandell.expense_splitter.entity.Expense;
import com.jonathansandell.expense_splitter.entity.Group;
import com.jonathansandell.expense_splitter.entity.Member;
import com.jonathansandell.expense_splitter.entity.SplitType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @DataJpaTest boots only the JPA slice (entity manager, repositories, an
 * in-memory DB) instead of the full application context that @SpringBootTest
 * would start. It's the right tool here: we're verifying that the entities
 * map to tables and round-trip correctly, not testing controllers or wiring.
 * Each test method runs in a transaction that's rolled back afterwards, so
 * tests don't leak data into each other.
 */
@DataJpaTest
class PersistenceSanityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Test
    void savesAndReloadsGroupWithMembersAndExpense() {
        Group group = groupRepository.save(new Group("Cabin Trip"));

        Member alice = memberRepository.save(new Member("Alice", group));
        Member bob = memberRepository.save(new Member("Bob", group));

        Expense expense = new Expense(
                "Groceries",
                new BigDecimal("50.00"),
                group,
                alice,
                Set.of(alice, bob)
        );
        expenseRepository.save(expense);

        // Force a round-trip to the actual database instead of reading back
        // the same managed objects still sitting in the persistence context.
        entityManager.flush();
        entityManager.clear();

        Group reloadedGroup = groupRepository.findById(group.getId()).orElseThrow();
        assertThat(reloadedGroup.getName()).isEqualTo("Cabin Trip");

        List<Member> members = memberRepository.findByGroupId(group.getId());
        assertThat(members).hasSize(2)
                .extracting(Member::getName)
                .containsExactlyInAnyOrder("Alice", "Bob");

        List<Expense> expenses = expenseRepository.findByGroupId(group.getId());
        assertThat(expenses).hasSize(1);

        Expense reloadedExpense = expenses.get(0);
        assertThat(reloadedExpense.getDescription()).isEqualTo("Groceries");
        assertThat(reloadedExpense.getAmount()).isEqualByComparingTo("50.00");
        assertThat(reloadedExpense.getSplitType()).isEqualTo(SplitType.EQUAL);
        assertThat(reloadedExpense.getPaidBy().getName()).isEqualTo("Alice");
        assertThat(reloadedExpense.getParticipants())
                .extracting(Member::getName)
                .containsExactlyInAnyOrder("Alice", "Bob");
    }
}
