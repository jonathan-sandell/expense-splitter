let currentGroupId = null;
let currentMembers = [];

const groupSetup = document.getElementById("group-setup");
const groupView = document.getElementById("group-view");
const groupTitle = document.getElementById("group-title");
const errorBanner = document.getElementById("error-banner");

const memberList = document.getElementById("member-list");
const expenseList = document.getElementById("expense-list");
const balanceList = document.getElementById("balance-list");
const settlementList = document.getElementById("settlement-list");
const paidBySelect = document.getElementById("expense-paid-by");
const participantsContainer = document.getElementById("expense-participants");

function showError(message) {
    errorBanner.textContent = message;
    errorBanner.classList.remove("hidden");
}

function clearError() {
    errorBanner.classList.add("hidden");
}

async function apiFetch(path, options) {
    const response = await fetch(path, options);
    if (!response.ok) {
        let message = `Request failed (${response.status})`;
        try {
            const body = await response.json();
            if (body.message) message = body.message;
        } catch {
            // response had no JSON body; keep the generic message
        }
        throw new Error(message);
    }
    if (response.status === 204) return null;
    return response.json();
}

function memberName(memberId) {
    const member = currentMembers.find((m) => m.id === memberId);
    return member ? member.name : `#${memberId}`;
}

function openGroup(id, name) {
    currentGroupId = id;
    groupTitle.textContent = `${name} (ID: ${id})`;
    groupSetup.classList.add("hidden");
    groupView.classList.remove("hidden");
    clearError();
    refreshAll();
}

function closeGroup() {
    currentGroupId = null;
    currentMembers = [];
    groupView.classList.add("hidden");
    groupSetup.classList.remove("hidden");
}

async function refreshAll() {
    await loadMembers();
    await Promise.all([loadExpenses(), loadBalances(), loadSettlement()]);
}

async function loadMembers() {
    currentMembers = await apiFetch(`/api/groups/${currentGroupId}/members`);

    memberList.innerHTML = "";
    for (const member of currentMembers) {
        const li = document.createElement("li");
        li.textContent = member.name;
        memberList.appendChild(li);
    }

    paidBySelect.innerHTML = "";
    participantsContainer.innerHTML = "";
    for (const member of currentMembers) {
        const option = document.createElement("option");
        option.value = member.id;
        option.textContent = member.name;
        paidBySelect.appendChild(option);

        const label = document.createElement("label");
        const checkbox = document.createElement("input");
        checkbox.type = "checkbox";
        checkbox.value = member.id;
        checkbox.checked = true;
        label.appendChild(checkbox);
        label.append(` ${member.name}`);
        participantsContainer.appendChild(label);
    }
}

async function loadExpenses() {
    const expenses = await apiFetch(`/api/groups/${currentGroupId}/expenses`);

    expenseList.innerHTML = "";
    if (expenses.length === 0) {
        const li = document.createElement("li");
        li.textContent = "No expenses yet.";
        expenseList.appendChild(li);
        return;
    }
    for (const expense of expenses) {
        const li = document.createElement("li");
        li.textContent = `${expense.description} — $${expense.amount} (paid by ${memberName(expense.paidByMemberId)})`;
        expenseList.appendChild(li);
    }
}

async function loadBalances() {
    const balances = await apiFetch(`/api/groups/${currentGroupId}/balances`);

    balanceList.innerHTML = "";
    for (const balance of balances) {
        const li = document.createElement("li");
        const amount = Number(balance.netBalance);
        if (amount > 0) {
            li.textContent = `${balance.memberName} is owed $${amount.toFixed(2)}`;
            li.classList.add("balance-positive");
        } else if (amount < 0) {
            li.textContent = `${balance.memberName} owes $${Math.abs(amount).toFixed(2)}`;
            li.classList.add("balance-negative");
        } else {
            li.textContent = `${balance.memberName} is settled up`;
        }
        balanceList.appendChild(li);
    }
}

async function loadSettlement() {
    const transfers = await apiFetch(`/api/groups/${currentGroupId}/settlement`);

    settlementList.innerHTML = "";
    if (transfers.length === 0) {
        const li = document.createElement("li");
        li.textContent = "Everyone is settled up.";
        settlementList.appendChild(li);
        return;
    }
    for (const transfer of transfers) {
        const li = document.createElement("li");
        li.textContent = `${transfer.fromMemberName} pays ${transfer.toMemberName} $${Number(transfer.amount).toFixed(2)}`;
        settlementList.appendChild(li);
    }
}

document.getElementById("create-group-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    clearError();
    const nameInput = document.getElementById("new-group-name");
    try {
        const group = await apiFetch("/api/groups", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name: nameInput.value }),
        });
        nameInput.value = "";
        openGroup(group.id, group.name);
    } catch (error) {
        showError(error.message);
    }
});

document.getElementById("load-group-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    clearError();
    const idInput = document.getElementById("load-group-id");
    try {
        const group = await apiFetch(`/api/groups/${idInput.value}`);
        openGroup(group.id, group.name);
    } catch (error) {
        showError(error.message);
    }
});

document.getElementById("back-button").addEventListener("click", closeGroup);

document.getElementById("add-member-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    clearError();
    const nameInput = document.getElementById("new-member-name");
    try {
        await apiFetch(`/api/groups/${currentGroupId}/members`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ name: nameInput.value }),
        });
        nameInput.value = "";
        await loadMembers();
    } catch (error) {
        showError(error.message);
    }
});

document.getElementById("add-expense-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    clearError();

    const descriptionInput = document.getElementById("expense-description");
    const amountInput = document.getElementById("expense-amount");
    const participantIds = Array.from(participantsContainer.querySelectorAll("input:checked")).map((c) => Number(c.value));

    if (participantIds.length === 0) {
        showError("Select at least one participant.");
        return;
    }

    try {
        await apiFetch(`/api/groups/${currentGroupId}/expenses`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                description: descriptionInput.value,
                amount: amountInput.value,
                paidByMemberId: Number(paidBySelect.value),
                participantMemberIds: participantIds,
            }),
        });
        descriptionInput.value = "";
        amountInput.value = "";
        await Promise.all([loadExpenses(), loadBalances(), loadSettlement()]);
    } catch (error) {
        showError(error.message);
    }
});

document.getElementById("refresh-balances").addEventListener("click", () => loadBalances().catch((e) => showError(e.message)));
document.getElementById("refresh-settlement").addEventListener("click", () => loadSettlement().catch((e) => showError(e.message)));
