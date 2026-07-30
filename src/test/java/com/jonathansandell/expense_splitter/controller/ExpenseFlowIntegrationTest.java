package com.jonathansandell.expense_splitter.controller;

import com.jonathansandell.expense_splitter.dto.AddMemberRequest;
import com.jonathansandell.expense_splitter.dto.CreateExpenseRequest;
import com.jonathansandell.expense_splitter.dto.CreateGroupRequest;
import com.jonathansandell.expense_splitter.dto.ExpenseResponse;
import com.jonathansandell.expense_splitter.dto.GroupResponse;
import com.jonathansandell.expense_splitter.dto.MemberResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the real HTTP endpoints end to end: create a group, add two
 * members, add an expense between them. This is storage-only for now
 * (no balance/settlement math yet) - just proving the wiring works.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class ExpenseFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void createsGroupAddsMembersAndAddsExpense() {
        ResponseEntity<GroupResponse> groupResponse = restTemplate.postForEntity(
                "/api/groups", new CreateGroupRequest("Cabin Trip"), GroupResponse.class);
        assertThat(groupResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long groupId = groupResponse.getBody().id();

        ResponseEntity<MemberResponse> aliceResponse = restTemplate.postForEntity(
                "/api/groups/{groupId}/members", new AddMemberRequest("Alice"), MemberResponse.class, groupId);
        ResponseEntity<MemberResponse> bobResponse = restTemplate.postForEntity(
                "/api/groups/{groupId}/members", new AddMemberRequest("Bob"), MemberResponse.class, groupId);
        assertThat(aliceResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(bobResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        Long aliceId = aliceResponse.getBody().id();
        Long bobId = bobResponse.getBody().id();

        CreateExpenseRequest expenseRequest = new CreateExpenseRequest(
                "Groceries", new BigDecimal("50.00"), aliceId, List.of(aliceId, bobId));
        ResponseEntity<ExpenseResponse> expenseResponse = restTemplate.postForEntity(
                "/api/groups/{groupId}/expenses", expenseRequest, ExpenseResponse.class, groupId);

        assertThat(expenseResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        ExpenseResponse expense = expenseResponse.getBody();
        assertThat(expense.description()).isEqualTo("Groceries");
        assertThat(expense.amount()).isEqualByComparingTo("50.00");
        assertThat(expense.paidByMemberId()).isEqualTo(aliceId);
        assertThat(expense.participantMemberIds()).containsExactlyInAnyOrder(aliceId, bobId);

        ResponseEntity<ExpenseResponse[]> listResponse = restTemplate.getForEntity(
                "/api/groups/{groupId}/expenses", ExpenseResponse[].class, groupId);
        assertThat(listResponse.getBody()).hasSize(1);
    }
}
