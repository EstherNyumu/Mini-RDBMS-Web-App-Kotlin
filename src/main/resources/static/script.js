let currentUser = null;
let isAdmin = false;

function renderTable(data) {
    if (!data || !data.length) {
        return "🔭 No data available";
    }
    const headers = Object.keys(data[0]);
    const columnWidths = headers.map(header => {
        const headerLength = header.length;
        const maxContentLength = Math.max(
            ...data.map(row => String(row[header]).length)
        );
        return Math.max(headerLength, maxContentLength) + 2;
    });
    const headerRow = headers.map((h, i) =>
        h.toUpperCase().padEnd(columnWidths[i])
    ).join(' │ ');
    const totalWidth = columnWidths.reduce((sum, w) => sum + w, 0) +
        (headers.length - 1) * 3;

    let html = '┌' + '─'.repeat(totalWidth + 2) + '┐\n';
    html += '│ ' + headerRow + ' │\n';
    html += '├' + '─'.repeat(totalWidth + 2) + '┤\n';

    html += data.map(row => {
        const rowData = headers.map((h, i) =>
            String(row[h]).padEnd(columnWidths[i])
        ).join(' │ ');
        return '│ ' + rowData + ' │';
    }).join('\n');

    html += '\n└' + '─'.repeat(totalWidth + 2) + '┘';

    html += `\n\n📊 Total: ${data.length} row${data.length !== 1 ? 's' : ''}`;

    return html;
}

function showMessage(elementId, message, type = 'info') {
    const element = document.getElementById(elementId);
    element.innerText = message;
    element.style.background = type === 'success' ? '#d1fae5' :
        type === 'error' ? '#fee2e2' : '#dbeafe';
    element.style.color = type === 'success' ? '#065f46' :
        type === 'error' ? '#991b1b' : '#1e40af';
    element.style.padding = '0.5rem';
    element.style.borderRadius = '4px';
    setTimeout(() => {
        element.innerText = '';
        element.style.padding = '0';
    }, 3000);
}

async function callJsonEndpoint(url, method = "GET", data = null) {
    try {
        const options = { method };
        if (data) {
            options.headers = { "Content-Type": "application/json" };
            options.body = JSON.stringify(data);
        }
        const res = await fetch(url, options);
        if (!res.ok) {
            throw new Error(`HTTP error! status: ${res.status}`);
        }
        if (res.headers.get("content-type")?.includes("application/json")) {
            return await res.json();
        } else {
            return await res.text();
        }
    } catch (error) {
        console.error('API Error:', error);
        return { error: error.message };
    }
}

function showDashboard(role) {
    document.getElementById('loginSection').style.display = 'none';

    if (role === 'admin') {
        document.getElementById('adminDashboard').style.display = 'grid';
        document.getElementById('userDashboard').style.display = 'none';
        loadAdminData();
    } else {
        document.getElementById('userDashboard').style.display = 'block';
        document.getElementById('adminDashboard').style.display = 'none';
        loadUserOrders();
    }

    updateUserInfo();
}

function updateUserInfo() {
    const userInfoDiv = document.getElementById('userInfo');
    if (isAdmin) {
        userInfoDiv.innerHTML = '<span class="user-badge admin">👑 Admin</span>';
    } else if (currentUser) {
        userInfoDiv.innerHTML = `<span class="user-badge user">👤 ${currentUser.name}</span>`;
    } else {
        userInfoDiv.innerHTML = '';
    }
}

async function userSignIn() {
    const name = document.getElementById('userName').value;
    const password = document.getElementById('userPassword').value;
    if (!name || !password) {
        showMessage("userLoginResult", "⚠️ Please fill in all fields", 'error');
        return false;
    }
    const existingUsers = await callJsonEndpoint("/users");
    if (Array.isArray(existingUsers)) {
        const user = existingUsers.find(u => u.name === name);
        if (user) {
            currentUser = user;
            isAdmin = false;
            showMessage("userLoginResult", `✅ Welcome back, ${name}!`, 'success');
            setTimeout(() => showDashboard('user'), 500);
        } else {
            const res = await callJsonEndpoint("/users", "POST", { name, password });

            if (res.error) {
                showMessage("userLoginResult", `❌ Error: ${res.error}`, 'error');
            } else {
                const updatedUsers = await callJsonEndpoint("/users");
                const newUser = updatedUsers.find(u => u.name === name);
                currentUser = newUser;
                isAdmin = false;
                showMessage("userLoginResult", `✅ Account created! Welcome, ${name}!`, 'success');
                setTimeout(() => showDashboard('user'), 500);
            }
        }
    }
    return false;
}

async function adminSignIn() {
    const password = document.getElementById('adminPassword').value;
    if (password === 'admin123') {
        isAdmin = true;
        currentUser = { name: 'Admin' };
        showMessage("adminLoginResult", `✅ Welcome, Admin!`, 'success');
        setTimeout(() => showDashboard('admin'), 500);
    } else {
        showMessage("adminLoginResult", `❌ Invalid admin password`, 'error');
    }
    return false;
}

function logout() {
    currentUser = null;
    isAdmin = false;
    document.getElementById('loginSection').style.display = 'block';
    document.getElementById('userDashboard').style.display = 'none';
    document.getElementById('adminDashboard').style.display = 'none';
    document.getElementById('userName').value = '';
    document.getElementById('userPassword').value = '';
    document.getElementById('adminPassword').value = '';
    updateUserInfo();
}

async function placeOrder() {
    if (!currentUser) {
        showMessage("placeOrderResult", "❌ Please sign in first", 'error');
        return false;
    }
    const res = await callJsonEndpoint("/orders", "POST", {
        userId: currentUser.id,
        status: "PENDING"
    });
    if (res.error) {
        showMessage("placeOrderResult", `❌ Error: ${res.error}`, 'error');
    } else {
        showMessage("placeOrderResult", `✅ Order placed successfully!`, 'success');
        await loadUserOrders();
    }
    return false;
}

async function loadUserOrders() {
    if (!currentUser) return;
    const res = await callJsonEndpoint("/orders");
    if (res.error) {
        document.getElementById("userOrdersList").innerText = `❌ Error loading orders: ${res.error}`;
    } else if (Array.isArray(res)) {
        const userOrders = res.filter(order => order.userId === currentUser.id);
        document.getElementById("userOrdersList").innerText = renderTable(userOrders);
    } else {
        document.getElementById("userOrdersList").innerText = "🔭 No orders found";
    }
}

async function cancelOrder() {
    const orderId = parseInt(document.getElementById("cancelOrderId").value);
    if (!orderId) {
        showMessage("cancelOrderResult", "⚠️ Please enter an Order ID", 'error');
        return false;
    }
    if (!confirm(`Are you sure you want to cancel order ${orderId}?`)) {
        return false;
    }
    const res = await callJsonEndpoint(`/orders/${orderId}`, "DELETE");
    if (res.error) {
        showMessage("cancelOrderResult", `❌ Error: ${res.error}`, 'error');
    } else {
        showMessage("cancelOrderResult", `✅ Order cancelled successfully!`, 'success');
        document.getElementById("cancelOrderId").value = '';
        await loadUserOrders();
    }
    return false;
}

async function loadAdminData() {
    await Promise.all([
        loadAdminUsers(),
        loadAdminOrders(),
        loadAdminJoin()
    ]);
}

async function loadAdminUsers() {
    const res = await callJsonEndpoint("/users");
    if (res.error) {
        document.getElementById("adminUsersList").innerText = `❌ Error loading users: ${res.error}`;
    } else if (Array.isArray(res)) {
        document.getElementById("adminUsersList").innerText = renderTable(res);
    } else {
        document.getElementById("adminUsersList").innerText = "🔭 No users found";
    }
}

async function loadAdminOrders() {
    const res = await callJsonEndpoint("/orders");
    if (res.error) {
        document.getElementById("adminOrdersList").innerText = `❌ Error loading orders: ${res.error}`;
    } else if (Array.isArray(res)) {
        document.getElementById("adminOrdersList").innerText = renderTable(res);
    } else {
        document.getElementById("adminOrdersList").innerText = "🔭 No orders found";
    }
}

async function loadAdminJoin() {
    const res = await callJsonEndpoint("/join");
    if (res.error) {
        document.getElementById("adminJoinResult").innerText = `❌ Error performing join: ${res.error}`;
    } else if (Array.isArray(res)) {
        document.getElementById("adminJoinResult").innerText = renderTable(res);
    } else {
        document.getElementById("adminJoinResult").innerText = "🔭 No joined data available";
    }
}

async function adminDeleteUser() {
    const userId = parseInt(document.getElementById("adminDeleteUserId").value);
    if (!userId) {
        showMessage("adminDeleteUserResult", "⚠️ Please enter a User ID", 'error');
        return false;
    }
    if (!confirm(`Are you sure you want to delete user ${userId}?`)) {
        return false;
    }
    const res = await callJsonEndpoint(`/users/${userId}`, "DELETE");
    if (res.error) {
        showMessage("adminDeleteUserResult", `❌ Error: ${res.error}`, 'error');
    } else {
        showMessage("adminDeleteUserResult", `✅ User deleted successfully!`, 'success');
        document.getElementById("adminDeleteUserId").value = '';
        await loadAdminData();
    }
    return false;
}

async function adminUpdateOrder() {
    const orderId = parseInt(document.getElementById("adminUpdateOrderId").value);
    const status = document.getElementById("adminUpdateOrderStatus").value;
    if (!orderId || !status) {
        showMessage("adminUpdateOrderResult", "⚠️ Please fill in all fields", 'error');
        return false;
    }
    const res = await callJsonEndpoint(`/orders/${orderId}`, "PUT", { status });
    if (res.error) {
        showMessage("adminUpdateOrderResult", `❌ Error: ${res.error}`, 'error');
    } else {
        showMessage("adminUpdateOrderResult", `✅ Order updated successfully!`, 'success');
        document.getElementById("adminUpdateOrderId").value = '';
        document.getElementById("adminUpdateOrderStatus").value = '';
        await loadAdminData();
    }
    return false;
}

async function adminDeleteOrder() {
    const orderId = parseInt(document.getElementById("adminDeleteOrderId").value);
    if (!orderId) {
        showMessage("adminDeleteOrderResult", "⚠️ Please enter an Order ID", 'error');
        return false;
    }
    if (!confirm(`Are you sure you want to delete order ${orderId}?`)) {
        return false;
    }
    const res = await callJsonEndpoint(`/orders/${orderId}`, "DELETE");
    if (res.error) {
        showMessage("adminDeleteOrderResult", `❌ Error: ${res.error}`, 'error');
    } else {
        showMessage("adminDeleteOrderResult", `✅ Order deleted successfully!`, 'success');
        document.getElementById("adminDeleteOrderId").value = '';
        await loadAdminData();
    }
    return false;
}
