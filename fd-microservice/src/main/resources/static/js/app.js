// Fixed Deposit Banking Portal - Core Client Logic

const API_BASE = '';
let currentToken = localStorage.getItem('fd_token') || '';
let currentUser = JSON.parse(localStorage.getItem('fd_user') || 'null');

// Product Cache
let availableProducts = [
  { productCode: 'FD_STD', productName: 'Standard Fixed Deposit', minRate: 5.00, maxRate: 7.50, minDeposit: 10000, minMonths: 3, maxMonths: 36, penaltyPct: 1.00, compounding: 'QUARTERLY' },
  { productCode: 'FD_PREM', productName: 'Premium Fixed Deposit', minRate: 6.00, maxRate: 8.50, minDeposit: 50000, minMonths: 12, maxMonths: 60, penaltyPct: 1.00, compounding: 'QUARTERLY' }
];

document.addEventListener('DOMContentLoaded', () => {
  initAuth();
  setupEventListeners();
  loadProducts();
  switchTab('create-account');
});

// Auth System
function initAuth() {
  updateUserUI();
}

function updateUserUI() {
  const userTag = document.getElementById('user-badge');
  const loginSection = document.getElementById('auth-section');
  if (currentUser && currentToken) {
    userTag.innerHTML = `
      <span class="mono-tag">${currentUser.username} (${currentUser.role.replace('ROLE_', '')})</span>
      <button class="btn btn-outline btn-sm" onclick="logout()">Logout</button>
    `;
    loginSection.style.display = 'none';
  } else {
    userTag.innerHTML = `<span style="color: var(--text-muted); font-size: 0.85rem;">Not Logged In</span>`;
    loginSection.style.display = 'block';
  }
}

async function quickLogin(username, password) {
  try {
    const res = await fetch(`${API_BASE}/api/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    const data = await res.json();
    if (res.ok) {
      currentToken = data.token;
      currentUser = { username: data.username, role: data.role, customerId: data.customerId };
      localStorage.setItem('fd_token', currentToken);
      localStorage.setItem('fd_user', JSON.stringify(currentUser));
      showToast(`Welcome back, ${data.username}!`, 'success');
      updateUserUI();
      if (document.getElementById('input-customer-id') && data.customerId) {
        document.getElementById('input-customer-id').value = data.customerId;
      }
      loadMyAccounts();
    } else {
      showToast(data.message || 'Login failed', 'error');
    }
  } catch (err) {
    showToast('Network error during login', 'error');
  }
}

function logout() {
  currentToken = '';
  currentUser = null;
  localStorage.removeItem('fd_token');
  localStorage.removeItem('fd_user');
  updateUserUI();
  showToast('Logged out successfully', 'info');
}

// Tab Navigation
function switchTab(tabId) {
  document.querySelectorAll('.tab-content').forEach(el => el.style.display = 'none');
  document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
  
  const targetContent = document.getElementById(`tab-${tabId}`);
  if (targetContent) targetContent.style.display = 'block';

  const targetBtn = document.querySelector(`[data-tab="${tabId}"]`);
  if (targetBtn) targetBtn.classList.add('active');

  if (tabId === 'my-accounts') loadMyAccounts();
  if (tabId === 'all-accounts') loadAllAccounts();
  if (tabId === 'reports') loadReports();
}

// Fetch Products from Backend
async function loadProducts() {
  try {
    const res = await fetch(`${API_BASE}/api/product/all`);
    if (res.ok) {
      const data = await res.json();
      if (data && data.length > 0) availableProducts = data;
    }
  } catch (e) {
    console.log('Using default product catalog cache');
  }
  populateProductDropdowns();
}

function populateProductDropdowns() {
  const select = document.getElementById('input-product');
  const calcSelect = document.getElementById('calc-product');
  if (!select) return;

  select.innerHTML = '';
  if (calcSelect) calcSelect.innerHTML = '';

  availableProducts.forEach(p => {
    const opt = document.createElement('option');
    opt.value = p.productCode;
    opt.textContent = `${p.productCode} - ${p.productName} (Min: ₹${p.minDeposit.toLocaleString()}, ${p.minRate}% - ${p.maxRate}%)`;
    select.appendChild(opt);
    if (calcSelect) calcSelect.appendChild(opt.cloneNode(true));
  });

  updateDynamicRatePreview();
}

// Setup Event Listeners
function setupEventListeners() {
  // Real-time calculation on form input changes
  ['input-principal', 'input-product', 'input-months'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.addEventListener('input', updateDynamicRatePreview);
  });

  document.querySelectorAll('.category-checkbox').forEach(cb => {
    cb.addEventListener('change', updateDynamicRatePreview);
  });

  // Account creation form submit
  const form = document.getElementById('fd-create-form');
  if (form) {
    form.addEventListener('submit', handleAccountCreation);
  }

  // Calculator form submit
  const calcForm = document.getElementById('fd-calc-form');
  if (calcForm) {
    calcForm.addEventListener('submit', handleCalculatorSubmit);
  }
}

// Dynamic Rate & Projection Preview (Before Form Submission)
async function updateDynamicRatePreview() {
  const productCode = document.getElementById('input-product')?.value || 'FD_STD';
  const principal = parseFloat(document.getElementById('input-principal')?.value) || 0;
  const termMonths = parseInt(document.getElementById('input-months')?.value) || 12;

  const categories = [];
  document.querySelectorAll('.category-checkbox:checked').forEach(cb => categories.push(cb.value));

  const product = availableProducts.find(p => p.productCode === productCode) || availableProducts[0];
  
  // Calculate category adjustments
  let baseRate = product.minRate || 5.00;
  let addon = 0;
  if (categories.includes('SENIOR_CITIZEN')) addon += 0.50;
  if (categories.includes('STAFF')) addon += 1.00;
  if (product.rateCapAddon && addon > product.rateCapAddon) addon = product.rateCapAddon;

  let effectiveRate = baseRate + addon;
  if (product.maxRate && effectiveRate > product.maxRate) effectiveRate = product.maxRate;

  // Local estimate of compound interest (Quarterly)
  const compoundingsPerYear = 4;
  const timeInYears = termMonths / 12;
  const ratePerPeriod = (effectiveRate / 100) / compoundingsPerYear;
  const totalPeriods = compoundingsPerYear * timeInYears;
  const maturityAmount = principal * Math.pow(1 + ratePerPeriod, totalPeriods);
  const estimatedInterest = Math.max(0, maturityAmount - principal);

  // Update UI Elements
  document.getElementById('preview-base-rate').textContent = `${baseRate.toFixed(2)}%`;
  document.getElementById('preview-addon').textContent = addon > 0 ? `+${addon.toFixed(2)}% (${categories.join(', ')})` : 'None (0.00%)';
  document.getElementById('preview-effective-rate').textContent = `${effectiveRate.toFixed(2)}% p.a.`;
  document.getElementById('preview-interest').textContent = `₹${estimatedInterest.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  document.getElementById('preview-maturity').textContent = `₹${(principal + estimatedInterest).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

// Account Creation Handler
async function handleAccountCreation(e) {
  e.preventDefault();
  if (!currentToken) {
    showToast('Please log in (e.g. as Officer or Admin) to create accounts', 'error');
    return;
  }

  const customerId = document.getElementById('input-customer-id').value.trim();
  const productCode = document.getElementById('input-product').value;
  const principalAmount = parseFloat(document.getElementById('input-principal').value);
  const termMonths = parseInt(document.getElementById('input-months').value);
  const branchCode = document.getElementById('input-branch').value.trim() || '001';

  const categories = [];
  document.querySelectorAll('.category-checkbox:checked').forEach(cb => categories.push(cb.value));

  const payload = {
    customerId,
    productCode,
    principalAmount,
    termMonths,
    branchCode,
    currency: 'INR',
    categories
  };

  try {
    const res = await fetch(`${API_BASE}/api/fd/account/create`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${currentToken}`
      },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (res.ok) {
      displayCreationSuccess(data);
      showToast('FD Account successfully created!', 'success');
      document.getElementById('fd-create-form').reset();
      updateDynamicRatePreview();
    } else {
      showToast(data.message || 'Failed to create account', 'error');
    }
  } catch (err) {
    showToast('Network error while opening account', 'error');
  }
}

// Transaction Success Display
function displayCreationSuccess(acct) {
  const container = document.getElementById('creation-success-container');
  container.innerHTML = `
    <div class="success-banner">
      <div style="font-size: 2.5rem; margin-bottom: 0.5rem;">🎉</div>
      <h3 style="font-size: 1.35rem; color: #10b981; margin-bottom: 0.5rem;">Fixed Deposit Created Successfully!</h3>
      <p style="color: var(--text-muted); font-size: 0.9rem; margin-bottom: 1.25rem;">
        Initial deposit transaction has been ledger-posted and verified.
      </p>

      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 1rem; text-align: left; background: rgba(0,0,0,0.3); padding: 1.25rem; border-radius: var(--radius-md); margin-bottom: 1.25rem;">
        <div>
          <span style="font-size: 0.8rem; color: var(--text-subtle); display: block;">FD Account Number</span>
          <span class="mono-tag" style="font-size: 1.1rem; font-weight: 700;">${acct.fdAccountNo}</span>
        </div>
        <div>
          <span style="font-size: 0.8rem; color: var(--text-subtle); display: block;">Initial Transaction ID</span>
          <span class="mono-tag" style="color: #34d399;">#TXN-${acct.initialTransactionId || '101'}</span>
        </div>
        <div>
          <span style="font-size: 0.8rem; color: var(--text-subtle); display: block;">Principal Amount</span>
          <strong style="color: #fff;">₹${acct.principalAmount.toLocaleString('en-IN')}</strong>
        </div>
        <div>
          <span style="font-size: 0.8rem; color: var(--text-subtle); display: block;">Interest Rate & Maturity</span>
          <strong style="color: #60a5fa;">${acct.interestRate}% (${acct.maturityDate})</strong>
        </div>
      </div>

      <div style="display: flex; justify-content: center; gap: 1rem;">
        <button class="btn btn-primary btn-sm" onclick="viewTransactions('${acct.fdAccountNo}')">📜 View Transaction Ledger</button>
        <button class="btn btn-outline btn-sm" onclick="switchTab('my-accounts')">📁 Go to My Portfolio</button>
      </div>
    </div>
  `;
  container.scrollIntoView({ behavior: 'smooth' });
}

// Load Customer Accounts
async function loadMyAccounts() {
  const tableBody = document.getElementById('my-accounts-table-body');
  if (!tableBody) return;

  if (!currentToken) {
    tableBody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-muted);">Please log in to view your FD accounts.</td></tr>`;
    return;
  }

  tableBody.innerHTML = `<tr><td colspan="7" style="text-align: center;">Loading accounts...</td></tr>`;

  try {
    const res = await fetch(`${API_BASE}/api/fd/accounts/my`, {
      headers: { 'Authorization': `Bearer ${currentToken}` }
    });
    const accounts = await res.json();

    if (!res.ok || accounts.length === 0) {
      tableBody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-muted);">No Fixed Deposit accounts found for your profile.</td></tr>`;
      return;
    }

    tableBody.innerHTML = accounts.map(a => `
      <tr>
        <td><span class="mono-tag">${a.fdAccountNo}</span></td>
        <td><strong>₹${a.principalAmount.toLocaleString('en-IN')}</strong></td>
        <td>${a.interestRate}% p.a.</td>
        <td>${a.tenureMonths} Mo</td>
        <td>${a.maturityDate}</td>
        <td>${getStatusBadge(a.status)}</td>
        <td>
          <div style="display: flex; gap: 0.4rem;">
            <button class="btn btn-outline btn-sm" onclick="viewTransactions('${a.fdAccountNo}')">History</button>
            <button class="btn btn-outline btn-sm" onclick="viewStatements('${a.fdAccountNo}')">Statements</button>
            ${a.status === 'ACTIVE' ? `
              <button class="btn btn-danger btn-sm" onclick="openWithdrawalModal('${a.fdAccountNo}', ${a.principalAmount}, ${a.interestRate})">
                ⚠️ Withdraw FD
              </button>
            ` : ''}
          </div>
        </td>
      </tr>
    `).join('');
  } catch (err) {
    tableBody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--danger);">Failed to load accounts.</td></tr>`;
  }
}

// Load All Accounts (Officer View)
async function loadAllAccounts() {
  const tableBody = document.getElementById('all-accounts-table-body');
  if (!tableBody) return;

  if (!currentToken) {
    tableBody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-muted);">Please log in as Bank Officer or Admin.</td></tr>`;
    return;
  }

  tableBody.innerHTML = `<tr><td colspan="7" style="text-align: center;">Loading all bank accounts...</td></tr>`;

  try {
    const res = await fetch(`${API_BASE}/api/fd/accounts/all`, {
      headers: { 'Authorization': `Bearer ${currentToken}` }
    });
    const accounts = await res.json();

    if (!res.ok || accounts.length === 0) {
      tableBody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--text-muted);">No accounts found.</td></tr>`;
      return;
    }

    tableBody.innerHTML = accounts.map(a => `
      <tr>
        <td><span class="mono-tag">${a.fdAccountNo}</span></td>
        <td><span class="mono-tag">${a.customerId}</span></td>
        <td><strong>₹${a.principalAmount.toLocaleString('en-IN')}</strong></td>
        <td>${a.interestRate}%</td>
        <td>₹${(a.accruedInterest || 0).toLocaleString('en-IN')}</td>
        <td>${getStatusBadge(a.status)}</td>
        <td>
          <button class="btn btn-outline btn-sm" onclick="viewTransactions('${a.fdAccountNo}')">Ledger</button>
        </td>
      </tr>
    `).join('');
  } catch (e) {
    tableBody.innerHTML = `<tr><td colspan="7" style="text-align: center; color: var(--danger);">Access restricted. Log in as Officer/Admin.</td></tr>`;
  }
}

function getStatusBadge(status) {
  if (status === 'ACTIVE') return `<span class="badge badge-active">ACTIVE</span>`;
  if (status === 'CLOSED') return `<span class="badge badge-closed">CLOSED</span>`;
  if (status === 'PREMATURE_CLOSED') return `<span class="badge badge-premature">PREMATURE CLOSED</span>`;
  return `<span class="badge">${status}</span>`;
}

// Pre-Maturity Withdrawal Modal & Interface
let selectedWithdrawalAccount = null;

function openWithdrawalModal(fdAccountNo, principal, rate) {
  selectedWithdrawalAccount = { fdAccountNo, principal, rate };
  document.getElementById('modal-withdraw-acct').textContent = fdAccountNo;
  document.getElementById('modal-withdraw-principal').textContent = `₹${principal.toLocaleString('en-IN')}`;
  document.getElementById('modal-withdraw-rate').textContent = `${rate}% p.a.`;

  // Set default withdrawal date to today
  const today = new Date().toISOString().split('T')[0];
  document.getElementById('withdraw-date-input').value = today;

  document.getElementById('withdrawal-modal').classList.add('show');
}

function closeWithdrawalModal() {
  document.getElementById('withdrawal-modal').classList.remove('show');
  selectedWithdrawalAccount = null;
}

async function confirmWithdrawal() {
  if (!selectedWithdrawalAccount || !currentToken) return;

  const withdrawalDate = document.getElementById('withdraw-date-input').value;
  const payload = {
    fdAccountNo: selectedWithdrawalAccount.fdAccountNo,
    withdrawalDate: withdrawalDate || null
  };

  try {
    const res = await fetch(`${API_BASE}/api/fd/account/withdraw`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${currentToken}`
      },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    closeWithdrawalModal();

    if (res.ok) {
      displayWithdrawalReceipt(data);
      showToast('Premature withdrawal successfully executed!', 'success');
      loadMyAccounts();
    } else {
      showToast(data.message || 'Withdrawal failed', 'error');
    }
  } catch (e) {
    showToast('Network error during withdrawal', 'error');
  }
}

function displayWithdrawalReceipt(data) {
  const container = document.getElementById('withdrawal-receipt-modal-body');
  container.innerHTML = `
    <div style="background: rgba(0,0,0,0.3); padding: 1.25rem; border-radius: var(--radius-md); margin-bottom: 1rem;">
      <div style="display: flex; justify-content: space-between; margin-bottom: 0.75rem;">
        <span style="color: var(--text-muted);">Account Number:</span>
        <span class="mono-tag">${data.fdAccountNo}</span>
      </div>
      <div style="display: flex; justify-content: space-between; margin-bottom: 0.75rem;">
        <span style="color: var(--text-muted);">Principal Returned:</span>
        <strong>₹${data.principalReturned?.toLocaleString('en-IN')}</strong>
      </div>
      <div style="display: flex; justify-content: space-between; margin-bottom: 0.75rem;">
        <span style="color: var(--text-muted);">Net Interest Earned:</span>
        <strong style="color: #34d399;">+ ₹${data.interestEarned?.toLocaleString('en-IN')}</strong>
      </div>
      <div style="display: flex; justify-content: space-between; margin-bottom: 0.75rem;">
        <span style="color: var(--text-muted);">Pre-Maturity Penalty Applied:</span>
        <strong style="color: #ef4444;">- ₹${data.penaltyApplied?.toLocaleString('en-IN')}</strong>
      </div>
      <div style="display: flex; justify-content: space-between; padding-top: 0.75rem; border-top: 1px solid var(--card-border); font-size: 1.1rem; font-weight: 700; color: #60a5fa;">
        <span>Final Payout Amount:</span>
        <span>₹${data.withdrawalAmount?.toLocaleString('en-IN')}</span>
      </div>
    </div>
  `;
  document.getElementById('withdrawal-receipt-modal').classList.add('show');
}

function closeReceiptModal() {
  document.getElementById('withdrawal-receipt-modal').classList.remove('show');
}

// Transaction Ledger Modal
async function viewTransactions(fdAccountNo) {
  const modal = document.getElementById('txn-modal');
  const tbody = document.getElementById('txn-table-body');
  document.getElementById('txn-modal-acct').textContent = fdAccountNo;
  tbody.innerHTML = `<tr><td colspan="5" style="text-align: center;">Loading ledger entries...</td></tr>`;
  modal.classList.add('show');

  try {
    const res = await fetch(`${API_BASE}/api/fd/account/${fdAccountNo}/transactions`, {
      headers: { 'Authorization': `Bearer ${currentToken}` }
    });
    const txns = await res.json();
    if (!res.ok || txns.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--text-muted);">No ledger entries found.</td></tr>`;
      return;
    }

    tbody.innerHTML = txns.map(t => `
      <tr>
        <td>#${t.txnId}</td>
        <td><span class="mono-tag" style="font-size: 0.75rem;">${t.txnType}</span></td>
        <td><strong>₹${t.amount?.toLocaleString('en-IN')}</strong></td>
        <td>${t.debitGlAccount} ➔ ${t.creditGlAccount}</td>
        <td style="font-size: 0.8rem; color: var(--text-muted);">${t.txnTimestamp || ''}</td>
      </tr>
    `).join('');
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--danger);">Failed to load transactions.</td></tr>`;
  }
}

function closeTxnModal() {
  document.getElementById('txn-modal').classList.remove('show');
}

// Statements Modal
async function viewStatements(fdAccountNo) {
  const modal = document.getElementById('stmt-modal');
  const tbody = document.getElementById('stmt-table-body');
  document.getElementById('stmt-modal-acct').textContent = fdAccountNo;
  tbody.innerHTML = `<tr><td colspan="5" style="text-align: center;">Loading statements...</td></tr>`;
  modal.classList.add('show');

  try {
    const res = await fetch(`${API_BASE}/api/fd/account/${fdAccountNo}/statements`, {
      headers: { 'Authorization': `Bearer ${currentToken}` }
    });
    const stmts = await res.json();
    if (!res.ok || stmts.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--text-muted);">No statements generated yet. Run monthly batch job.</td></tr>`;
      return;
    }

    tbody.innerHTML = stmts.map(s => `
      <tr>
        <td>${s.statementDate}</td>
        <td>₹${s.openingBalance?.toLocaleString('en-IN')}</td>
        <td style="color: #34d399;">+ ₹${s.interestCredited?.toLocaleString('en-IN')}</td>
        <td><strong>₹${s.closingBalance?.toLocaleString('en-IN')}</strong></td>
      </tr>
    `).join('');
  } catch (e) {
    tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--danger);">Failed to load statements.</td></tr>`;
  }
}

function closeStmtModal() {
  document.getElementById('stmt-modal').classList.remove('show');
}

// Public Calculator Simulation
async function handleCalculatorSubmit(e) {
  e.preventDefault();
  const principal = parseFloat(document.getElementById('calc-principal').value);
  const baseRate = parseFloat(document.getElementById('calc-rate').value);
  const termMonths = parseInt(document.getElementById('calc-months').value);
  const compoundingFrequency = document.getElementById('calc-freq').value;

  const categories = [];
  if (document.getElementById('calc-cat-senior').checked) categories.push('SENIOR_CITIZEN');
  if (document.getElementById('calc-cat-staff').checked) categories.push('STAFF');

  const payload = {
    principal,
    baseRate,
    termMonths,
    compoundingFrequency,
    categories,
    calculationType: 'COMPOUND'
  };

  try {
    const res = await fetch(`${API_BASE}/api/fd/calculator/simulate`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (res.ok) {
      document.getElementById('calc-res-rate').textContent = `${data.effectiveRate}% p.a.`;
      document.getElementById('calc-res-interest').textContent = `₹${data.interestEarned?.toLocaleString('en-IN')}`;
      document.getElementById('calc-res-maturity').textContent = `₹${data.maturityAmount?.toLocaleString('en-IN')}`;
      document.getElementById('calc-result-box').style.display = 'block';
    }
  } catch (err) {
    showToast('Simulation failed', 'error');
  }
}

// Reports
async function loadReports() {
  const container = document.getElementById('reports-container');
  if (!container) return;

  if (!currentToken) {
    container.innerHTML = `<p style="color: var(--text-muted); text-align: center;">Please log in as Officer/Admin to view bank-wide reports.</p>`;
    return;
  }

  try {
    const res = await fetch(`${API_BASE}/api/report/fd-summary`, {
      headers: { 'Authorization': `Bearer ${currentToken}` }
    });
    const summaries = await res.json();
    if (res.ok) {
      container.innerHTML = `
        <div style="margin-bottom: 1.5rem; display: flex; justify-content: flex-end;">
          <a href="${API_BASE}/api/report/export/csv" class="btn btn-outline btn-sm" target="_blank" download>📥 Export CSV Summary</a>
        </div>
        <div class="table-responsive">
          <table>
            <thead>
              <tr>
                <th>Product Code</th>
                <th>Product Name</th>
                <th>Total Accounts</th>
                <th>Active</th>
                <th>Closed</th>
                <th>Total Principal Held</th>
                <th>Total Interest Accrued</th>
              </tr>
            </thead>
            <tbody>
              ${summaries.map(s => `
                <tr>
                  <td><span class="mono-tag">${s.productCode}</span></td>
                  <td>${s.productName}</td>
                  <td>${s.totalAccounts}</td>
                  <td><span class="badge badge-active">${s.activeAccounts}</span></td>
                  <td><span class="badge badge-closed">${s.closedAccounts}</span></td>
                  <td><strong>₹${s.totalPrincipal?.toLocaleString('en-IN')}</strong></td>
                  <td style="color: #34d399;">₹${s.totalInterestAccrued?.toLocaleString('en-IN')}</td>
                </tr>
              `).join('')}
            </tbody>
          </table>
        </div>
      `;
    }
  } catch (e) {
    container.innerHTML = `<p style="color: var(--danger);">Failed to load reports.</p>`;
  }
}

// Toast System
function showToast(msg, type = 'info') {
  const container = document.getElementById('toast-container');
  if (!container) return;
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = msg;
  container.appendChild(toast);
  setTimeout(() => toast.remove(), 4000);
}
