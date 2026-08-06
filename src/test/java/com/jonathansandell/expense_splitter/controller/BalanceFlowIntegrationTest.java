package com.jonathansandell.expense_splitter.controller;

import com.jonathansandell.expense_splitter.dto.AddMemberRequest;
import com.jonathansandell.expense_splitter.dto.CreateExpenseRequest;
import com.jonathansandell.expense_splitter.dto.CreateGroupRequest;
import com.jonathansandell.expense_splitter.dto.ErrorResponse;
import com.jonathansandell.expense_splitter.dto.GroupResponse;
import com.jonathansandell.expense_splitter.dto.MemberBalanceResponse;
import com.jonathansandell.expense_splitter.dto.MemberResponse;
import com.jonathansandell.expense_splitter.dto.TransferResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Drives the balances/settlement endpoints end to end, plus the error
 * paths (validation failure, not-found) that GlobalExceptionHandler covers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class BalanceFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void computesBalancesAndSettlementForAGroup() {
        Long groupId = createGroup("Cabin Trip").id();
        Long aliceId = addMember(groupId, "Alice").id();
        Long bobId = addMember(groupId, "Bob").id();

        ResponseEntity<Void> expenseResponse = restTemplate.postForEntity(
                "/api/groups/{groupId}/expenses",
                new CreateExpenseRequest("Groceries", new BigDecimal("100.00"), aliceId, List.of(aliceId, bobId)),
                Void.class, groupId);
        assertThat(expenseResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<MemberBalanceResponse[]> balancesResponse = restTemplate.getForEntity(
                "/api/groups/{groupId}/balances", MemberBalanceResponse[].class, groupId);
        assertThat(balancesResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<MemberBalanceResponse> balances = List.of(balancesResponse.getBody());
        assertThat(balances).hasSize(2);
        assertThat(balances).anySatisfy(b -> {
            assertThat(b.memberId()).isEqualTo(aliceId);
            assertThat(b.netBalance()).isEqualByComparingTo("50.00");
        });
        assertThat(balances).anySatisfy(b -> {
            assertThat(b.memberId()).isEqualTo(bobId);
            assertThat(b.netBalance()).isEqualByComparingTo("-50.00");
        });

        ResponseEntity<TransferResponse[]> settlementResponse = restTemplate.getForEntity(
                "/api/groups/{groupId}/settlement", TransferResponse[].class, groupId);
        assertThat(settlementResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<TransferResponse> transfers = List.of(settlementResponse.getBody());
        assertThat(transfers).hasSize(1);
        TransferResponse transfer = transfers.get(0);
        assertThat(transfer.fromMemberId()).isEqualTo(bobId);
        assertThat(transfer.toMemberId()).isEqualTo(aliceId);
        assertThat(transfer.amount()).isEqualByComparingTo("50.00");
    }

    @Test
    void balancesForUnknownGroupReturnsNotFound() {
        ResponseEntity<ErrorResponse> response = restTemplate.getForEntity(
                "/api/groups/{groupId}/balances", ErrorResponse.class, 999_999L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).contains("999999");
    }

    @Test
    void creatingGroupWithBlankNameReturnsBadRequest() {
        ResponseEntity<ErrorResponse> response = restTemplate.postForEntity(
                "/api/groups", new CreateGroupRequest(""), ErrorResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().message()).contains("name");
    }

    private GroupResponse createGroup(String name) {
        return restTemplate.postForEntity("/api/groups", new CreateGroupRequest(name), GroupResponse.class).getBody();
    }

    private MemberResponse addMember(Long groupId, String name) {
        return restTemplate.postForEntity(
                "/api/groups/{groupId}/members", new AddMemberRequest(name), MemberResponse.class, groupId).getBody();
    }
}