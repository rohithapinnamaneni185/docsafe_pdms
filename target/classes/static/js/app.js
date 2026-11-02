/**
 * DocuSafe - Personal Document Management System (PDMS)
 * Multi-Member Vault & Frontend Application Controller
 */
document.addEventListener('DOMContentLoaded', () => {
    // Current application state
    const state = {
        currentUser: null,
        currentView: 'dashboard',
        viewFilter: 'all', // 'all', 'valid', 'expiring', 'expired'
        documents: [],
        familyMembers: [],
        selectedMemberFilter: '', // empty = all members
        currentDocDetail: null,
        deleteTargetId: null,
        isEditing: false,
        searchDebounceTimer: null,
        displayMode: 'grid', // 'grid' or 'table'
        selectedFile: null,
        removeExistingImage: false,
        
        // Soft-Delete & Trash System
        trashDocuments: [],
        permanentDeleteTargetId: null,

        // Remove Member Modal State
        removeMemberTargetId: null,
        removeMemberTargetName: '',

        // Expiry Calendar System
        calendarMonth: new Date().getMonth(),
        calendarYear: new Date().getFullYear(),
        calendarDocs: [],

        // Notifications & Alerts
        dashboardSummary: null,
        hasShownExpiringToast: false,
        hoverTimer: null,

        // Theme Engine
        currentTheme: 'theme-default',

        // Two-Factor Authentication & Login State
        tempToken: null,
        otpTimer: null,
        otpCountdown: 30
    };

    // DOM Elements Cache
    const el = {
        // Main App Layout & Theme
        appMainContainer: document.getElementById('appMainContainer'),
        sidebar: document.getElementById('sidebar'),
        mobileMenuToggle: document.getElementById('mobileMenuToggle'),
        navItems: document.querySelectorAll('.sidebar-nav .nav-item'),
        brandLogoBtn: document.getElementById('brandLogoBtn'),
        headerQuickAddBtn: document.getElementById('headerQuickAddBtn'),
        headerVaultSelect: document.getElementById('headerVaultSelect'),
        pageTitle: document.getElementById('pageTitle'),
        pageSubtitle: document.getElementById('pageSubtitle'),
        currentDateDisplay: document.getElementById('currentDateDisplay'),
        btnThemeToggle: document.getElementById('btnThemeToggle'),

        // User Profile & Badges
        sidebarUserAvatar: document.getElementById('sidebarUserAvatar'),
        sidebarUserName: document.getElementById('sidebarUserName'),
        sidebarUserRole: document.getElementById('sidebarUserRole'),
        headerUserBadge: document.getElementById('headerUserBadge'),
        headerRoleBadge: document.getElementById('headerRoleBadge'),
        headerUserNameDisplay: document.getElementById('headerUserNameDisplay'),

        // Sidebar Badges
        sidebarBadgeTotal: document.getElementById('sidebarBadgeTotal'),
        sidebarBadgeExpiring: document.getElementById('sidebarBadgeExpiring'),
        sidebarBadgeExpired: document.getElementById('sidebarBadgeExpired'),
        sidebarBadgeMembers: document.getElementById('sidebarBadgeMembers'),
        sidebarBadgeTrash: document.getElementById('sidebarBadgeTrash'),

        // Page Views
        viewDashboard: document.getElementById('viewDashboard'),
        viewDocuments: document.getElementById('viewDocuments'),
        viewCalendar: document.getElementById('viewCalendar'),
        viewTrash: document.getElementById('viewTrash'),
        viewThemeSettings: document.getElementById('viewThemeSettings'),
        viewFamilyMembers: document.getElementById('viewFamilyMembers'),
        viewAddDocument: document.getElementById('viewAddDocument'),

        // Dashboard Elements
        adminScopeBar: document.getElementById('adminScopeBar'),
        dashboardMemberFilter: document.getElementById('dashboardMemberFilter'),
        statTotalDocs: document.getElementById('statTotalDocs'),
        statValidDocs: document.getElementById('statValidDocs'),
        statExpiringDocs: document.getElementById('statExpiringDocs'),
        statExpiredDocs: document.getElementById('statExpiredDocs'),
        cardTotalDocs: document.getElementById('cardTotalDocs'),
        cardValidDocs: document.getElementById('cardValidDocs'),
        cardExpiringDocs: document.getElementById('cardExpiringDocs'),
        cardExpiredDocs: document.getElementById('cardExpiredDocs'),
        dashboardExpiringBody: document.getElementById('dashboardExpiringBody'),
        dashboardExpiringEmpty: document.getElementById('dashboardExpiringEmpty'),
        dashboardRecentBody: document.getElementById('dashboardRecentBody'),
        dashboardRecentEmpty: document.getElementById('dashboardRecentEmpty'),
        categoryDistributionContainer: document.getElementById('categoryDistributionContainer'),
        btnViewAllExpiring: document.getElementById('btnViewAllExpiring'),
        btnViewAllRecent: document.getElementById('btnViewAllRecent'),
        emptyStateAddBtn: document.getElementById('emptyStateAddBtn'),

        // Calendar View Elements
        btnCalPrev: document.getElementById('btnCalPrev'),
        btnCalToday: document.getElementById('btnCalToday'),
        btnCalNext: document.getElementById('btnCalNext'),
        calendarMonthTitle: document.getElementById('calendarMonthTitle'),
        calendarDaysGrid: document.getElementById('calendarDaysGrid'),
        calendarDateModal: document.getElementById('calendarDateModal'),
        calModalTitle: document.getElementById('calModalTitle'),
        calModalDocsList: document.getElementById('calModalDocsList'),

        // Trash View Elements
        trashTable: document.getElementById('trashTable'),
        trashTableBody: document.getElementById('trashTableBody'),
        trashEmptyState: document.getElementById('trashEmptyState'),
        trashCountDisplay: document.getElementById('trashCountDisplay'),
        permanentDeleteConfirmModal: document.getElementById('permanentDeleteConfirmModal'),
        permDeleteDocName: document.getElementById('permDeleteDocName'),
        btnConfirmPermanentDelete: document.getElementById('btnConfirmPermanentDelete'),

        // Theme Settings Elements
        themeCardDefault: document.getElementById('themeCardDefault'),
        themeCardWhite: document.getElementById('themeCardWhite'),
        themeCardDark: document.getElementById('themeCardDark'),

        // Expiring Soon Hover Alert Popover
        expiringHoverPopover: document.getElementById('expiringHoverPopover'),
        expiringHoverPopoverBody: document.getElementById('expiringHoverPopoverBody'),
        navExpiring: document.getElementById('navExpiring'),

        // Documents View Elements
        docSearchInput: document.getElementById('docSearchInput'),
        docFilterMember: document.getElementById('docFilterMember'),
        docFilterCategory: document.getElementById('docFilterCategory'),
        docFilterStatus: document.getElementById('docFilterStatus'),
        docSortOrder: document.getElementById('docSortOrder'),
        btnClearFilters: document.getElementById('btnClearFilters'),
        btnViewGrid: document.getElementById('btnViewGrid'),
        btnViewTable: document.getElementById('btnViewTable'),
        documentsGridContainer: document.getElementById('documentsGridContainer'),
        documentsTableContainer: document.getElementById('documentsTableContainer'),
        allDocsTableBody: document.getElementById('allDocsTableBody'),
        documentsCountLabel: document.getElementById('documentsCountLabel'),
        documentsEmptyState: document.getElementById('documentsEmptyState'),
        btnEmptyResetFilters: document.getElementById('btnEmptyResetFilters'),

        // Family Members View Elements
        btnOpenAddMemberModal: document.getElementById('btnOpenAddMemberModal'),
        membersGridContainer: document.getElementById('membersGridContainer'),
        addMemberModal: document.getElementById('addMemberModal'),
        addMemberForm: document.getElementById('addMemberForm'),
        memberFullName: document.getElementById('memberFullName'),
        memberRelationship: document.getElementById('memberRelationship'),
        memberEmail: document.getElementById('memberEmail'),
        memberPassword: document.getElementById('memberPassword'),
        btnSaveMember: document.getElementById('btnSaveMember'),

        // Form Elements
        documentForm: document.getElementById('documentForm'),
        docIdField: document.getElementById('docIdField'),
        formSectionTitle: document.getElementById('formSectionTitle'),
        formSubmitText: document.getElementById('formSubmitText'),
        docMemberGroup: document.getElementById('docMemberGroup'),
        docMemberSelect: document.getElementById('docMemberSelect'),
        docNameInput: document.getElementById('docNameInput'),
        docCategorySelect: document.getElementById('docCategorySelect'),
        docOrgInput: document.getElementById('docOrgInput'),
        docRefInput: document.getElementById('docRefInput'),
        docIssueDateInput: document.getElementById('docIssueDateInput'),
        docExpiryDateInput: document.getElementById('docExpiryDateInput'),
        noExpiryCheckbox: document.getElementById('noExpiryCheckbox'),
        docNotesInput: document.getElementById('docNotesInput'),
        uploadDropzone: document.getElementById('uploadDropzone'),
        fileUploadInput: document.getElementById('fileUploadInput'),
        uploadPreviewBox: document.getElementById('uploadPreviewBox'),
        previewImageEl: document.getElementById('previewImageEl'),
        previewFileName: document.getElementById('previewFileName'),
        previewFileSize: document.getElementById('previewFileSize'),
        btnRemoveUploadedImage: document.getElementById('btnRemoveUploadedImage'),
        btnCancelForm: document.getElementById('btnCancelForm'),

        // Modals
        viewDetailModal: document.getElementById('viewDetailModal'),
        detailDocTitle: document.getElementById('detailDocTitle'),
        detailImageSection: document.getElementById('detailImageSection'),
        detailImageEl: document.getElementById('detailImageEl'),
        detailDownloadBtn: document.getElementById('detailDownloadBtn'),
        detailOwnerCell: document.getElementById('detailOwnerCell'),
        detailStatusCell: document.getElementById('detailStatusCell'),
        detailCategoryCell: document.getElementById('detailCategoryCell'),
        detailOrgCell: document.getElementById('detailOrgCell'),
        detailRefCell: document.getElementById('detailRefCell'),
        detailIssueDateCell: document.getElementById('detailIssueDateCell'),
        detailExpiryDateCell: document.getElementById('detailExpiryDateCell'),
        detailDaysCell: document.getElementById('detailDaysCell'),
        detailNotesCell: document.getElementById('detailNotesCell'),
        detailCreatedCell: document.getElementById('detailCreatedCell'),
        detailEditBtn: document.getElementById('detailEditBtn'),
        detailDeleteBtn: document.getElementById('detailDeleteBtn'),

        deleteConfirmModal: document.getElementById('deleteConfirmModal'),
        deleteTargetDocName: document.getElementById('deleteTargetDocName'),
        btnConfirmDelete: document.getElementById('btnConfirmDelete'),

        // Edit Family Member Modal
        editMemberModal: document.getElementById('editMemberModal'),
        editMemberForm: document.getElementById('editMemberForm'),
        editMemberId: document.getElementById('editMemberId'),
        editMemberFullName: document.getElementById('editMemberFullName'),
        editMemberRelationship: document.getElementById('editMemberRelationship'),
        editMemberEmail: document.getElementById('editMemberEmail'),
        btnSaveEditMember: document.getElementById('btnSaveEditMember'),

        // Premium Subscription Upgrade Modal
        premiumUpgradeModal: document.getElementById('premiumUpgradeModal'),
        btnUpgradePremium: document.getElementById('btnUpgradePremium'),

        // Remove Family Member Modal
        removeMemberModal: document.getElementById('removeMemberModal'),
        removeMemberTargetName: document.getElementById('removeMemberTargetName'),
        btnConfirmRemoveMember: document.getElementById('btnConfirmRemoveMember'),

        // Two-Factor Authentication & Login Elements
        loginOverlay: document.getElementById('loginOverlay'),
        loginStep1Card: document.getElementById('loginStep1Card'),
        loginStep2Card: document.getElementById('loginStep2Card'),
        formLoginStep1: document.getElementById('formLoginStep1'),
        loginIdentifier: document.getElementById('loginIdentifier'),
        loginPassword: document.getElementById('loginPassword'),
        btnInitiateLogin: document.getElementById('btnInitiateLogin'),
        btnFillDemoCreds: document.getElementById('btnFillDemoCreds'),
        formLoginStep2: document.getElementById('formLoginStep2'),
        otpDestinationText: document.getElementById('otpDestinationText'),
        otpInput: document.getElementById('otpInput'),
        btnVerifyOtp: document.getElementById('btnVerifyOtp'),
        otpTimerText: document.getElementById('otpTimerText'),
        otpCountdownWrapper: document.getElementById('otpCountdownWrapper'),
        btnResendOtp: document.getElementById('btnResendOtp'),
        btnBackToStep1: document.getElementById('btnBackToStep1'),
        devOtpHelperBadge: document.getElementById('devOtpHelperBadge'),
        devOtpDisplay: document.getElementById('devOtpDisplay'),
        btnFillDevOtp: document.getElementById('btnFillDevOtp'),
        btnLogout: document.getElementById('btnLogout'),

        toastContainer: document.getElementById('toastContainer')
    };

    /* ------------------------------------------------------------------
       INITIALIZATION & VAULT MANAGEMENT
       ------------------------------------------------------------------ */
    init();

    async function init() {
        setupThemeEngine();
        setupCurrentDate();
        setupNavigation();
        setupFilterListeners();
        setupFormHandling();
        setupFileUpload();
        setupFamilyMemberModals();
        setupModals();
        setupVaultSwitcher();
        setupCalendarView();
        setupTrashView();
        setupExpiringHoverAlerts();
        setupAuthWorkflow();

        // Check active session and authenticate if required
        await loadInitialUser();
    }

    function setupCurrentDate() {
        const options = { weekday: 'short', year: 'numeric', month: 'short', day: 'numeric' };
        const today = new Date();
        if (el.currentDateDisplay) {
            el.currentDateDisplay.textContent = today.toLocaleDateString('en-US', options);
        }
    }

    /* ------------------------------------------------------------------
       ACTIVE VAULT & USER PROFILE HANDLING
       ------------------------------------------------------------------ */
    async function loadInitialUser() {
        try {
            const res = await API.getMe();
            if (res && res.success && res.data) {
                hideLoginOverlay();
                setActiveUser(res.data);
            } else {
                showLoginOverlay();
            }
        } catch (error) {
            console.info('No active session, presenting sign in screen.');
            showLoginOverlay();
        }
    }

    /* ------------------------------------------------------------------
       TWO-STEP AUTHENTICATION & OTP WORKFLOW (2FA)
       ------------------------------------------------------------------ */
    function showLoginOverlay() {
        if (el.appMainContainer) {
            el.appMainContainer.style.display = 'none';
        }
        if (el.loginOverlay) {
            el.loginOverlay.style.display = 'flex';
        }
        if (el.loginStep1Card) el.loginStep1Card.style.display = 'block';
        if (el.loginStep2Card) el.loginStep2Card.style.display = 'none';
        if (state.otpTimer) {
            clearInterval(state.otpTimer);
            state.otpTimer = null;
        }
        if (el.loginPassword) el.loginPassword.value = '';
    }

    function hideLoginOverlay() {
        if (el.loginOverlay) {
            el.loginOverlay.style.display = 'none';
        }
        if (el.appMainContainer) {
            el.appMainContainer.style.display = 'flex';
        }
        if (state.otpTimer) {
            clearInterval(state.otpTimer);
            state.otpTimer = null;
        }
    }

    function startOtpCountdown() {
        if (state.otpTimer) {
            clearInterval(state.otpTimer);
            state.otpTimer = null;
        }
        state.otpCountdown = 30;
        if (el.otpTimerText) el.otpTimerText.textContent = '30s';
        if (el.otpCountdownWrapper) el.otpCountdownWrapper.style.display = 'inline';
        if (el.btnResendOtp) el.btnResendOtp.style.display = 'none';

        state.otpTimer = setInterval(() => {
            state.otpCountdown--;
            if (el.otpTimerText) el.otpTimerText.textContent = `${state.otpCountdown}s`;
            if (state.otpCountdown <= 0) {
                clearInterval(state.otpTimer);
                state.otpTimer = null;
                if (el.otpCountdownWrapper) el.otpCountdownWrapper.style.display = 'none';
                if (el.btnResendOtp) el.btnResendOtp.style.display = 'inline-block';
            }
        }, 1000);
    }

    function setupAuthWorkflow() {
        // Step 1: Submit email/mobile and password
        if (el.formLoginStep1) {
            el.formLoginStep1.addEventListener('submit', async (e) => {
                e.preventDefault();
                const identifier = el.loginIdentifier.value.trim();
                const password = el.loginPassword.value;

                if (!identifier || !password) {
                    showToast('Please enter your email or mobile number and password.', 'warning');
                    return;
                }

                const originalBtnHtml = el.btnInitiateLogin.innerHTML;
                el.btnInitiateLogin.disabled = true;
                el.btnInitiateLogin.innerHTML = '<span>Sending Code...</span>';

                try {
                    const res = await API.loginInitiate(identifier, password);
                    if (res && res.success && res.data) {
                        state.tempToken = res.data.tempToken;
                        
                        if (el.otpDestinationText) {
                            el.otpDestinationText.textContent = res.data.destination || identifier;
                        }

                        // Demo Helper simulation display
                        if (res.data.debugOtp && el.devOtpDisplay) {
                            el.devOtpDisplay.textContent = res.data.debugOtp;
                        }

                        // Transition to Step 2
                        if (el.loginStep1Card) el.loginStep1Card.style.display = 'none';
                        if (el.loginStep2Card) el.loginStep2Card.style.display = 'block';
                        if (el.otpInput) {
                            el.otpInput.value = '';
                            el.otpInput.focus();
                        }

                        startOtpCountdown();
                        showToast('6-digit verification code dispatched!', 'success');
                    } else {
                        showToast((res && res.message) || 'Authentication failed. Please check your credentials.', 'error');
                    }
                } catch (error) {
                    showToast(error.message || 'Unable to authenticate. Please check your credentials.', 'error');
                } finally {
                    el.btnInitiateLogin.disabled = false;
                    el.btnInitiateLogin.innerHTML = originalBtnHtml;
                }
            });
        }

        // Autofill Demo Administrator Credentials
        if (el.btnFillDemoCreds) {
            el.btnFillDemoCreds.addEventListener('click', () => {
                if (el.loginIdentifier) el.loginIdentifier.value = 'admin@docusafe.local';
                if (el.loginPassword) el.loginPassword.value = 'admin123';
                showToast('Autofilled demo administrator credentials', 'info');
                if (el.loginIdentifier) el.loginIdentifier.focus();
            });
        }

        // Auto-paste Simulated Demo OTP
        if (el.btnFillDevOtp) {
            el.btnFillDevOtp.addEventListener('click', () => {
                const code = el.devOtpDisplay ? el.devOtpDisplay.textContent.trim() : '';
                if (code && code !== '------' && el.otpInput) {
                    el.otpInput.value = code;
                    el.otpInput.focus();
                    showToast('Pasted demo OTP code: ' + code, 'info');
                }
            });
        }

        // Step 2: Submit 6-digit OTP
        if (el.formLoginStep2) {
            el.formLoginStep2.addEventListener('submit', async (e) => {
                e.preventDefault();
                const otp = el.otpInput.value.trim();

                if (!state.tempToken) {
                    showToast('Session token expired. Please return to login.', 'warning');
                    if (el.loginStep2Card) el.loginStep2Card.style.display = 'none';
                    if (el.loginStep1Card) el.loginStep1Card.style.display = 'block';
                    return;
                }

                if (!otp || otp.length !== 6) {
                    showToast('Please enter the full 6-digit verification code.', 'warning');
                    return;
                }

                const originalBtnHtml = el.btnVerifyOtp.innerHTML;
                el.btnVerifyOtp.disabled = true;
                el.btnVerifyOtp.innerHTML = '<span>Verifying...</span>';

                try {
                    const res = await API.verifyOtp(state.tempToken, otp);
                    if (res && res.success && res.data) {
                        showToast(`Welcome back, ${res.data.fullName}!`, 'success');
                        hideLoginOverlay();
                        setActiveUser(res.data);
                    } else {
                        showToast((res && res.message) || 'Invalid or expired verification code.', 'error');
                        if (el.otpInput) el.otpInput.select();
                    }
                } catch (error) {
                    showToast(error.message || 'Verification failed. Please check the code.', 'error');
                    if (el.otpInput) el.otpInput.select();
                } finally {
                    el.btnVerifyOtp.disabled = false;
                    el.btnVerifyOtp.innerHTML = originalBtnHtml;
                }
            });
        }

        // Resend OTP
        if (el.btnResendOtp) {
            el.btnResendOtp.addEventListener('click', async () => {
                if (!state.tempToken) {
                    showToast('Session expired. Please return to login.', 'warning');
                    return;
                }

                el.btnResendOtp.disabled = true;
                el.btnResendOtp.textContent = 'Resending...';

                try {
                    const res = await API.resendOtp(state.tempToken);
                    if (res && res.success && res.data) {
                        if (res.data.debugOtp && el.devOtpDisplay) {
                            el.devOtpDisplay.textContent = res.data.debugOtp;
                        }
                        showToast('Fresh 6-digit verification code sent!', 'success');
                        startOtpCountdown();
                        if (el.otpInput) {
                            el.otpInput.value = '';
                            el.otpInput.focus();
                        }
                    } else {
                        showToast((res && res.message) || 'Failed to resend code.', 'error');
                    }
                } catch (error) {
                    showToast(error.message || 'Failed to resend OTP.', 'error');
                } finally {
                    el.btnResendOtp.disabled = false;
                    el.btnResendOtp.textContent = 'Resend Code';
                }
            });
        }

        // Back to Step 1
        if (el.btnBackToStep1) {
            el.btnBackToStep1.addEventListener('click', () => {
                if (state.otpTimer) {
                    clearInterval(state.otpTimer);
                    state.otpTimer = null;
                }
                state.tempToken = null;
                if (el.loginStep2Card) el.loginStep2Card.style.display = 'none';
                if (el.loginStep1Card) el.loginStep1Card.style.display = 'block';
                if (el.loginIdentifier) el.loginIdentifier.focus();
            });
        }

        // Sidebar Logout Button
        if (el.btnLogout) {
            el.btnLogout.addEventListener('click', async () => {
                try {
                    await API.logout();
                } catch (ignored) {
                }
                state.currentUser = null;
                showToast('You have been logged out of DocuSafe.', 'info');
                showLoginOverlay();
            });
        }

        // Listen for unauthorized 401 events from any API call
        window.addEventListener('auth:unauthorized', () => {
            if (state.currentUser) {
                state.currentUser = null;
                showToast('Your session has expired. Please sign in again.', 'warning');
            }
            showLoginOverlay();
        });
    }

    function setActiveUser(user) {
        state.currentUser = user;

        // Update Profile in Sidebar
        const initials = getInitials(user.fullName);
        if (el.sidebarUserAvatar) el.sidebarUserAvatar.textContent = initials;
        if (el.sidebarUserName) el.sidebarUserName.textContent = user.fullName;
        if (el.sidebarUserRole) el.sidebarUserRole.textContent = user.admin ? 'Administrator (Self)' : `Member (${user.relationship})`;

        // Update Header Vault Select value
        if (el.headerVaultSelect && user.id) {
            el.headerVaultSelect.value = user.id;
        }

        // Admin-specific UI adjustments
        if (user.admin) {
            if (el.adminScopeBar) el.adminScopeBar.style.display = 'flex';
            if (el.docFilterMember) el.docFilterMember.style.display = 'inline-block';
            if (el.docMemberGroup) el.docMemberGroup.style.display = 'flex';
            if (el.btnOpenAddMemberModal) el.btnOpenAddMemberModal.style.display = 'inline-flex';
        } else {
            if (el.adminScopeBar) el.adminScopeBar.style.display = 'none';
            if (el.docFilterMember) el.docFilterMember.style.display = 'none';
            if (el.docMemberGroup) el.docMemberGroup.style.display = 'none';
            if (el.btnOpenAddMemberModal) el.btnOpenAddMemberModal.style.display = 'none';
        }

        // Load data for active vault
        loadFamilyMembers();
        loadDashboard();
        if (state.currentView === 'documents') {
            loadDocuments();
        }
    }

    function setupVaultSwitcher() {
        if (el.headerVaultSelect) {
            el.headerVaultSelect.addEventListener('change', async (e) => {
                const targetUserId = e.target.value;
                if (!targetUserId) return;
                await window.switchActiveVault(targetUserId);
            });
        }
    }

    window.switchActiveVault = async function(userId) {
        try {
            const res = await API.switchVault(userId);
            if (res.success && res.data) {
                showToast('Vault Switched', `Now managing ${res.data.fullName}'s Vault (${res.data.relationship || 'Self'})`, 'success');
                setActiveUser(res.data);
            }
        } catch (err) {
            showToast('Switch Error', err.message, 'error');
        }
    };

    function getInitials(name) {
        if (!name) return 'DS';
        const parts = name.trim().split(/\s+/);
        if (parts.length >= 2) {
            return (parts[0][0] + parts[1][0]).toUpperCase();
        }
        return parts[0].substring(0, 2).toUpperCase();
    }

    /* ------------------------------------------------------------------
       NAVIGATION & VIEW SWITCHING
       ------------------------------------------------------------------ */
    function setupNavigation() {
        // Mobile sidebar toggle
        if (el.mobileMenuToggle) {
            el.mobileMenuToggle.addEventListener('click', () => {
                el.sidebar.classList.toggle('open');
            });
        }

        document.addEventListener('click', (e) => {
            if (window.innerWidth <= 768 && el.sidebar.classList.contains('open')) {
                if (!el.sidebar.contains(e.target) && !el.mobileMenuToggle.contains(e.target)) {
                    el.sidebar.classList.remove('open');
                }
            }
        });

        el.navItems.forEach(item => {
            item.addEventListener('click', () => {
                const view = item.getAttribute('data-view');
                const filter = item.getAttribute('data-filter') || 'all';
                switchView(view, filter);
                if (window.innerWidth <= 768) {
                    el.sidebar.classList.remove('open');
                }
            });
        });

        if (el.brandLogoBtn) {
            el.brandLogoBtn.addEventListener('click', (e) => {
                e.preventDefault();
                switchView('dashboard');
            });
        }

        if (el.headerQuickAddBtn) el.headerQuickAddBtn.addEventListener('click', () => resetAndOpenAddForm());
        if (el.emptyStateAddBtn) el.emptyStateAddBtn.addEventListener('click', () => resetAndOpenAddForm());

        if (el.cardTotalDocs) el.cardTotalDocs.addEventListener('click', () => switchView('documents', 'all'));
        if (el.cardValidDocs) el.cardValidDocs.addEventListener('click', () => switchView('documents', 'valid'));
        if (el.cardExpiringDocs) el.cardExpiringDocs.addEventListener('click', () => switchView('documents', 'expiring'));
        if (el.cardExpiredDocs) el.cardExpiredDocs.addEventListener('click', () => switchView('documents', 'expired'));

        if (el.btnViewAllExpiring) el.btnViewAllExpiring.addEventListener('click', () => switchView('documents', 'expiring'));
        if (el.btnViewAllRecent) el.btnViewAllRecent.addEventListener('click', () => switchView('documents', 'all'));

        // Dashboard member filter (Admin)
        if (el.dashboardMemberFilter) {
            el.dashboardMemberFilter.addEventListener('change', (e) => {
                state.selectedMemberFilter = e.target.value;
                loadDashboard();
            });
        }
    }

    function switchView(viewName, filter = 'all') {
        state.currentView = viewName;
        state.viewFilter = filter;

        document.querySelectorAll('.page-view').forEach(view => view.classList.remove('active'));

        el.navItems.forEach(item => {
            item.classList.remove('active');
            const itemView = item.getAttribute('data-view');
            const itemFilter = item.getAttribute('data-filter') || 'all';
            if (itemView === viewName && (itemView !== 'documents' || itemFilter === filter)) {
                item.classList.add('active');
            }
        });

        switch (viewName) {
            case 'dashboard':
                el.pageTitle.textContent = 'Dashboard';
                el.pageSubtitle.textContent = state.currentUser && state.currentUser.admin ? 
                    'Personal & family document vault' : 'Personal document vault & expiry tracker';
                el.viewDashboard.classList.add('active');
                loadDashboard();
                break;
            case 'documents':
                el.viewDocuments.classList.add('active');
                applyViewFilter(filter);
                loadDocuments();
                break;
            case 'calendar':
                el.pageTitle.textContent = 'Expiry Calendar';
                el.pageSubtitle.textContent = 'Visual monthly timeline of upcoming document renewals and deadlines';
                el.viewCalendar.classList.add('active');
                loadCalendarView();
                break;
            case 'trash':
                el.pageTitle.textContent = 'Recycle Bin';
                el.pageSubtitle.textContent = 'Safely restore soft-deleted items or permanently remove them from disk';
                el.viewTrash.classList.add('active');
                loadTrashDocuments();
                break;
            case 'theme-settings':
                el.pageTitle.textContent = 'Theme Settings';
                el.pageSubtitle.textContent = 'Personalize dashboard colors, page background, and dark mode';
                el.viewThemeSettings.classList.add('active');
                updateThemeSelectionUI();
                break;
            case 'family-members':
                el.pageTitle.textContent = 'Family Members';
                el.pageSubtitle.textContent = 'Manage household profiles and separate login accounts';
                el.viewFamilyMembers.classList.add('active');
                loadFamilyMembers();
                break;
            case 'add-document':
                el.viewAddDocument.classList.add('active');
                break;
        }
    }

    function applyViewFilter(filter) {
        if (filter === 'expiring') {
            el.pageTitle.textContent = 'Expiring Soon';
            el.pageSubtitle.textContent = 'Documents expiring within the next 30 days';
            el.docFilterStatus.value = 'EXPIRING_SOON';
        } else if (filter === 'expired') {
            el.pageTitle.textContent = 'Expired Documents';
            el.pageSubtitle.textContent = 'Documents that have passed their renewal deadline';
            el.docFilterStatus.value = 'EXPIRED';
        } else if (filter === 'valid') {
            el.pageTitle.textContent = 'Valid Documents';
            el.pageSubtitle.textContent = 'Active documents with more than 30 days validity';
            el.docFilterStatus.value = 'VALID';
        } else {
            el.pageTitle.textContent = 'All Documents';
            el.pageSubtitle.textContent = 'Manage, search, and organize all stored records';
            el.docFilterStatus.value = 'ALL';
        }
    }

    /* ------------------------------------------------------------------
       DASHBOARD DATA LOADER
       ------------------------------------------------------------------ */
    async function loadDashboard() {
        if (!state.currentUser) return;
        const memberId = state.currentUser.admin ? state.selectedMemberFilter : null;

        try {
            const res = await API.getDashboardSummary(memberId);
            if (res.success && res.data) {
                const data = res.data;
                state.dashboardSummary = data;

                el.statTotalDocs.textContent = data.totalDocuments;
                el.statValidDocs.textContent = data.validDocuments;
                el.statExpiringDocs.textContent = data.expiringSoonDocuments;
                el.statExpiredDocs.textContent = data.expiredDocuments;

                el.sidebarBadgeTotal.textContent = data.totalDocuments;
                el.sidebarBadgeExpiring.textContent = data.expiringSoonDocuments;
                el.sidebarBadgeExpired.textContent = data.expiredDocuments;
                if (el.sidebarBadgeTrash) {
                    el.sidebarBadgeTrash.textContent = data.trashDocuments !== undefined ? data.trashDocuments : 0;
                }

                // Smooth slide-out toast on dashboard load if documents are expiring soon
                if (data.expiringSoonDocuments > 0 && !state.hasShownExpiringToast) {
                    state.hasShownExpiringToast = true;
                    const count = data.expiringSoonDocuments;
                    showToast('⚠️ Expiring Soon Alert', `${count} document${count > 1 ? 's are' : ' is'} expiring within 30 days! Review now.`, 'warning');
                }

                renderCategoryDistribution(data.categoryCounts, data.totalDocuments);
                renderExpiringTable(data.expiringSoonList);
                renderRecentTable(data.recentDocuments);
            }
        } catch (error) {
            showToast('Connection Error', error.message, 'error');
        }
    }

    function renderCategoryDistribution(categoryCounts, totalDocs) {
        if (!el.categoryDistributionContainer) return;
        el.categoryDistributionContainer.innerHTML = '';

        if (!categoryCounts || Object.keys(categoryCounts).length === 0) {
            el.categoryDistributionContainer.innerHTML = '<p style="color:var(--text-muted); font-size:0.85rem;">No categories available.</p>';
            return;
        }

        const entries = Object.entries(categoryCounts);
        entries.forEach(([category, count]) => {
            const percentage = totalDocs > 0 ? Math.round((count / totalDocs) * 100) : 0;
            const item = document.createElement('div');
            item.className = 'category-bar-item';
            item.innerHTML = `
                <div class="category-bar-header">
                    <span class="category-name">${escapeHtml(category)}</span>
                    <span class="category-count">${count} (${percentage}%)</span>
                </div>
                <div class="progress-track">
                    <div class="progress-fill" style="width: ${percentage}%;"></div>
                </div>
            `;
            el.categoryDistributionContainer.appendChild(item);
        });
    }

    function renderExpiringTable(docs) {
        if (!el.dashboardExpiringBody) return;
        el.dashboardExpiringBody.innerHTML = '';

        if (!docs || docs.length === 0) {
            document.getElementById('dashboardExpiringTable').style.display = 'none';
            el.dashboardExpiringEmpty.style.display = 'flex';
            return;
        }

        document.getElementById('dashboardExpiringTable').style.display = 'table';
        el.dashboardExpiringEmpty.style.display = 'none';

        docs.forEach(doc => {
            const row = document.createElement('tr');
            const daysText = doc.daysRemaining === 0 ? 'Expires today' : `Expires in ${doc.daysRemaining} days`;
            row.innerHTML = `
                <td>
                    <div class="doc-name-cell">
                        ${renderThumbnailHtml(doc)}
                        <div>
                            <span class="doc-text-title">${escapeHtml(doc.documentName)}</span>
                            <span class="doc-text-subtitle">${escapeHtml(doc.issuingOrganization || '—')}</span>
                        </div>
                    </div>
                </td>
                <td><span class="doc-owner-badge">${escapeHtml(doc.userName || 'Self')}</span></td>
                <td>${getCategoryBadgeHtml(doc.category)}</td>
                <td><strong>${formatDate(doc.expiryDate)}</strong></td>
                <td><span class="badge badge-warning">${daysText}</span></td>
                <td>
                    <button class="btn btn-secondary btn-sm" onclick="window.viewDoc(${doc.id})">View</button>
                </td>
            `;
            el.dashboardExpiringBody.appendChild(row);
        });
    }

    function renderRecentTable(docs) {
        if (!el.dashboardRecentBody) return;
        el.dashboardRecentBody.innerHTML = '';

        if (!docs || docs.length === 0) {
            document.getElementById('dashboardRecentTable').style.display = 'none';
            el.dashboardRecentEmpty.style.display = 'flex';
            return;
        }

        document.getElementById('dashboardRecentTable').style.display = 'table';
        el.dashboardRecentEmpty.style.display = 'none';

        docs.forEach(doc => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>
                    <div class="doc-name-cell">
                        ${renderThumbnailHtml(doc)}
                        <div>
                            <span class="doc-text-title">${escapeHtml(doc.documentName)}</span>
                            <span class="doc-text-subtitle">${doc.referenceNumber ? '#' + escapeHtml(doc.referenceNumber) : '—'}</span>
                        </div>
                    </div>
                </td>
                <td><span class="doc-owner-badge">${escapeHtml(doc.userName || 'Self')}</span></td>
                <td>${getCategoryBadgeHtml(doc.category)}</td>
                <td>${escapeHtml(doc.issuingOrganization || '—')}</td>
                <td>${doc.expiryDate ? formatDate(doc.expiryDate) : '<span style="color:var(--text-muted);">No Expiry</span>'}</td>
                <td>${getStatusBadgeHtml(doc)}</td>
                <td>
                    <div style="display: flex; gap: 6px;">
                        <button class="btn btn-icon" onclick="window.viewDoc(${doc.id})" title="View Details">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                <circle cx="12" cy="12" r="3"></circle>
                            </svg>
                        </button>
                        <button class="btn btn-icon" onclick="window.editDoc(${doc.id})" title="Edit Document">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                            </svg>
                        </button>
                    </div>
                </td>
            `;
            el.dashboardRecentBody.appendChild(row);
        });
    }

    /* ------------------------------------------------------------------
       FAMILY MEMBERS MANAGEMENT
       ------------------------------------------------------------------ */
    async function loadFamilyMembers() {
        try {
            const res = await API.getFamilyMembers();
            if (res.success && res.data) {
                state.familyMembers = res.data;
                el.sidebarBadgeMembers.textContent = state.familyMembers.length;

                renderFamilyMembers(state.familyMembers);
                populateMemberDropdowns(state.familyMembers);
            }
        } catch (error) {
            console.error('Failed to load family members:', error);
        }
    }

    function renderFamilyMembers(members) {
        if (!el.membersGridContainer) return;
        el.membersGridContainer.innerHTML = '';

        members.forEach(member => {
            const card = document.createElement('div');
            card.className = 'member-card';
            const initials = getInitials(member.fullName);
            const isSelf = state.currentUser && state.currentUser.id === member.id;
            const isAdminMember = Boolean(member.admin || (member.role && member.role === 'ROLE_ADMIN') || (member.relationship && member.relationship.toLowerCase() === 'self'));
            const canEdit = (state.currentUser && state.currentUser.admin) || isSelf;
            const canRemove = Boolean(state.currentUser && state.currentUser.admin && !isSelf && !isAdminMember);

            card.innerHTML = `
                <div class="member-card-header">
                    <div class="member-avatar">${initials}</div>
                    <div class="member-info">
                        <h3>${escapeHtml(member.fullName)}</h3>
                        <div class="member-email">${escapeHtml(member.email)}</div>
                    </div>
                </div>

                <div style="display: flex; gap: 8px;">
                    <span class="rel-badge ${member.admin ? 'admin' : ''}">${escapeHtml(member.relationship)}</span>
                    <span class="badge ${member.admin ? 'badge-success' : 'badge-neutral'}">${member.admin ? 'Vault Admin' : 'Family Member'}</span>
                </div>

                <div class="member-stats-row">
                    <span style="color:var(--text-muted); font-size:0.8rem;">Assigned Documents</span>
                    <strong>${member.documentCount} ${member.documentCount === 1 ? 'record' : 'records'}</strong>
                </div>

                <div class="member-card-actions member-actions card-actions">
                    <button class="btn btn-primary btn-sm btn-manage-vault" onclick="window.switchActiveVault(${member.id})">
                        Manage Vault
                    </button>
                    ${canEdit ? `
                        <button class="btn-icon" title="Edit Profile" onclick="editFamilyMember(${member.id})">
                            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                            </svg>
                        </button>
                    ` : ''}
                    ${canRemove ? `
                        <button class="btn btn-sm btn-remove-member" title="Remove Member" onclick="deleteFamilyMember(${member.id}, '${escapeQuote(member.fullName)}')">
                            Remove
                        </button>
                    ` : ''}
                </div>
            `;
            el.membersGridContainer.appendChild(card);
        });
    }

    function populateMemberDropdowns(members) {
        // 1. Header Active Vault Switcher
        if (el.headerVaultSelect) {
            el.headerVaultSelect.innerHTML = '';
            members.forEach(m => {
                const opt = document.createElement('option');
                opt.value = m.id;
                opt.textContent = `${m.fullName} (${m.relationship || (m.admin ? 'Self' : 'Member')})`;
                if (state.currentUser && m.id === state.currentUser.id) {
                    opt.selected = true;
                }
                el.headerVaultSelect.appendChild(opt);
            });
            if (state.currentUser && state.currentUser.id) {
                el.headerVaultSelect.value = state.currentUser.id;
            }
        }

        // 2. Dashboard Filter (Admin)
        if (el.dashboardMemberFilter) {
            const currentVal = el.dashboardMemberFilter.value;
            el.dashboardMemberFilter.innerHTML = '<option value="">All Family Members</option>';
            members.forEach(m => {
                const opt = document.createElement('option');
                opt.value = m.id;
                opt.textContent = `${m.fullName} (${m.relationship})`;
                el.dashboardMemberFilter.appendChild(opt);
            });
            el.dashboardMemberFilter.value = currentVal;
        }

        // 3. All Documents Filter (Admin)
        if (el.docFilterMember) {
            const currentVal = el.docFilterMember.value;
            el.docFilterMember.innerHTML = '<option value="">All Family Members</option>';
            members.forEach(m => {
                const opt = document.createElement('option');
                opt.value = m.id;
                opt.textContent = `${m.fullName} (${m.relationship})`;
                el.docFilterMember.appendChild(opt);
            });
            el.docFilterMember.value = currentVal;
        }

        // 4. Add Document Form Assignment (Admin)
        if (el.docMemberSelect) {
            el.docMemberSelect.innerHTML = '';
            members.forEach(m => {
                const opt = document.createElement('option');
                opt.value = m.id;
                opt.textContent = `${m.fullName} (${m.relationship})`;
                if (state.currentUser && m.id === state.currentUser.id) {
                    opt.selected = true;
                }
                el.docMemberSelect.appendChild(opt);
            });
        }
    }

    function getNonSelfMembersCount() {
        if (!state.familyMembers || !Array.isArray(state.familyMembers)) return 0;
        return state.familyMembers.filter(m => {
            const rel = (m.relationship || '').toLowerCase().trim();
            return rel !== 'self' && !m.admin;
        }).length;
    }

    function setupFamilyMemberModals() {
        if (el.btnOpenAddMemberModal) {
            el.btnOpenAddMemberModal.addEventListener('click', () => {
                if (getNonSelfMembersCount() >= 3) {
                    openModal(el.premiumUpgradeModal);
                    return;
                }
                el.addMemberForm.reset();
                clearValidationErrors();
                openModal(el.addMemberModal);
            });
        }

        // Add Member Form Submission
        el.addMemberForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (getNonSelfMembersCount() >= 3) {
                closeModal(el.addMemberModal);
                openModal(el.premiumUpgradeModal);
                return;
            }

            const fullName = el.memberFullName.value.trim();
            const relationship = el.memberRelationship.value;
            const email = el.memberEmail.value.trim();
            const password = el.memberPassword.value.trim();

            if (!fullName || !email || !password) {
                showToast('Validation Error', 'Please fill in all required fields.', 'error');
                return;
            }

            el.btnSaveMember.disabled = true;
            el.btnSaveMember.textContent = 'Creating Account...';

            try {
                const res = await API.addFamilyMember({ fullName, relationship, email, password });
                showToast('Member Added', `Account for '${res.data.fullName}' created successfully.`, 'success');
                closeModal(el.addMemberModal);
                await loadFamilyMembers();
                await loadDashboard();
            } catch (error) {
                if (error.status === 403 || (error.message && error.message.toLowerCase().includes('limit reached'))) {
                    closeModal(el.addMemberModal);
                    openModal(el.premiumUpgradeModal);
                } else {
                    showToast('Add Member Failed', error.message, 'error');
                }
            } finally {
                el.btnSaveMember.disabled = false;
                el.btnSaveMember.textContent = 'Save Family Member';
            }
        });

        // Edit Member Profile Modal Opener
        window.openEditMemberModal = function(memberId) {
            const member = state.familyMembers.find(m => m.id === memberId);
            if (!member) {
                showToast('Error', 'Family member not found', 'error');
                return;
            }

            el.editMemberId.value = member.id;
            el.editMemberFullName.value = member.fullName || '';
            el.editMemberRelationship.value = member.relationship || (member.admin ? 'Self' : 'Other');
            el.editMemberEmail.value = member.email || '';
            clearValidationErrors();
            openModal(el.editMemberModal);
        };
        window.editFamilyMember = window.openEditMemberModal;

        // Edit Member Form Submission
        if (el.editMemberForm) {
            el.editMemberForm.addEventListener('submit', async (e) => {
                e.preventDefault();
                const memberId = el.editMemberId.value;
                const fullName = el.editMemberFullName.value.trim();
                const relationship = el.editMemberRelationship.value;
                const email = el.editMemberEmail.value.trim();

                if (!fullName || !email) {
                    showToast('Validation Error', 'Full name and email are required.', 'error');
                    return;
                }

                el.btnSaveEditMember.disabled = true;
                el.btnSaveEditMember.textContent = 'Updating Profile...';

                try {
                    const res = await API.updateFamilyMember(memberId, { fullName, relationship, email });
                    showToast('Profile Updated', `Profile for '${res.data.fullName}' updated successfully.`, 'success');
                    closeModal(el.editMemberModal);

                    // If the updated user is currently the active session user, update active view displays immediately
                    if (state.currentUser && state.currentUser.id === parseInt(memberId, 10)) {
                        state.currentUser.fullName = res.data.fullName;
                        state.currentUser.email = res.data.email;
                        state.currentUser.relationship = res.data.relationship;

                        const initials = getInitials(res.data.fullName);
                        if (el.sidebarUserAvatar) el.sidebarUserAvatar.textContent = initials;
                        if (el.sidebarUserName) el.sidebarUserName.textContent = res.data.fullName;
                        if (el.sidebarUserRole) el.sidebarUserRole.textContent = res.data.admin ? 'Administrator (Self)' : `Member (${res.data.relationship})`;
                    }

                    await loadFamilyMembers();
                    await loadDashboard();
                    if (state.currentView === 'documents') {
                        loadDocuments();
                    }
                } catch (error) {
                    showToast('Update Failed', error.message, 'error');
                } finally {
                    el.btnSaveEditMember.disabled = false;
                    el.btnSaveEditMember.textContent = 'Update Profile';
                }
            });
        }

        window.viewMemberDocs = function(memberId) {
            if (state.currentUser && state.currentUser.admin) {
                el.docFilterMember.value = memberId;
            }
            switchView('documents', 'all');
        };

        window.deleteMember = function(memberId, memberName) {
            state.removeMemberTargetId = memberId;
            state.removeMemberTargetName = memberName;

            if (el.removeMemberModal) {
                if (el.removeMemberTargetName) {
                    el.removeMemberTargetName.textContent = `'${memberName}'`;
                }
                openModal(el.removeMemberModal);
            } else {
                if (confirm(`Are you sure you want to remove '${memberName}'? Documents linked to this member will be unassigned or removed.`)) {
                    (async () => {
                        try {
                            await API.deleteFamilyMember(memberId);
                            showToast('Member Removed', `'${memberName}' was removed successfully.`, 'success');
                            if (state.currentUser && state.currentUser.id === parseInt(memberId, 10)) {
                                await loadMe();
                            }
                            await loadFamilyMembers();
                            await loadDashboard();
                            if (state.currentView === 'documents') {
                                loadDocuments();
                            }
                        } catch (error) {
                            showToast('Remove Failed', error.message, 'error');
                        }
                    })();
                }
            }
        };
        window.deleteFamilyMember = window.deleteMember;
        window.removeFamilyMember = window.deleteMember;
    }

    /* ------------------------------------------------------------------
       DOCUMENTS VIEW & FILTERS
       ------------------------------------------------------------------ */
    function setupFilterListeners() {
        el.docSearchInput.addEventListener('input', () => {
            clearTimeout(state.searchDebounceTimer);
            state.searchDebounceTimer = setTimeout(() => {
                loadDocuments();
            }, 300);
        });

        if (el.docFilterMember) el.docFilterMember.addEventListener('change', () => loadDocuments());
        el.docFilterCategory.addEventListener('change', () => loadDocuments());
        el.docFilterStatus.addEventListener('change', () => loadDocuments());
        el.docSortOrder.addEventListener('change', () => loadDocuments());

        el.btnClearFilters.addEventListener('click', resetFilters);
        el.btnEmptyResetFilters.addEventListener('click', resetFilters);

        el.btnViewGrid.addEventListener('click', () => setDisplayMode('grid'));
        el.btnViewTable.addEventListener('click', () => setDisplayMode('table'));
    }

    function resetFilters() {
        el.docSearchInput.value = '';
        if (el.docFilterMember) el.docFilterMember.value = '';
        el.docFilterCategory.value = '';
        el.docFilterStatus.value = 'ALL';
        el.docSortOrder.value = 'createdAt,desc';
        loadDocuments();
    }

    function setDisplayMode(mode) {
        state.displayMode = mode;
        if (mode === 'grid') {
            el.btnViewGrid.classList.add('active');
            el.btnViewTable.classList.remove('active');
            el.documentsGridContainer.style.display = 'grid';
            el.documentsTableContainer.style.display = 'none';
        } else {
            el.btnViewGrid.classList.remove('active');
            el.btnViewTable.classList.add('active');
            el.documentsGridContainer.style.display = 'none';
            el.documentsTableContainer.style.display = 'block';
        }
    }

    async function loadDocuments() {
        const query = el.docSearchInput.value.trim();
        const category = el.docFilterCategory.value;
        const status = el.docFilterStatus.value;
        const sort = el.docSortOrder.value;
        const memberId = (state.currentUser && state.currentUser.admin) ? el.docFilterMember.value : null;

        try {
            const res = await API.getDocuments({ query, category, status, memberId, sort });
            if (res.success && res.data) {
                state.documents = res.data;
                renderDocuments(state.documents);
            }
        } catch (error) {
            showToast('Failed to load documents', error.message, 'error');
        }
    }

    function renderDocuments(docs) {
        el.documentsCountLabel.textContent = `Showing ${docs.length} ${docs.length === 1 ? 'document' : 'documents'}`;

        if (docs.length === 0) {
            el.documentsGridContainer.style.display = 'none';
            el.documentsTableContainer.style.display = 'none';
            el.documentsEmptyState.style.display = 'flex';
            return;
        }

        el.documentsEmptyState.style.display = 'none';
        setDisplayMode(state.displayMode);

        // 1. Grid Cards
        el.documentsGridContainer.innerHTML = '';
        docs.forEach(doc => {
            const card = document.createElement('div');
            card.className = 'doc-card';
            card.innerHTML = `
                <div class="doc-card-image-wrapper">
                    ${doc.imageUrl ? 
                        `<img src="${doc.imageUrl}" class="doc-card-image" alt="${escapeHtml(doc.documentName)}" onclick="window.viewDoc(${doc.id})">` : 
                        `<div class="doc-card-placeholder-img" onclick="window.viewDoc(${doc.id})" style="cursor:pointer;">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                <polyline points="14 2 14 8 20 8"></polyline>
                            </svg>
                            <span>No scan attached</span>
                        </div>`
                    }
                    <div class="doc-card-category-badge">${getCategoryBadgeHtml(doc.category)}</div>
                    <div class="doc-card-status-badge">${getStatusBadgeHtml(doc)}</div>
                </div>

                <div class="doc-card-body">
                    <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; margin-bottom: 4px;">
                        <h3 class="doc-card-title">${escapeHtml(doc.documentName)}</h3>
                    </div>
                    
                    <div style="margin-bottom: 8px;">
                        <span class="doc-owner-badge" title="Document belongs to">
                            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path>
                                <circle cx="12" cy="7" r="4"></circle>
                            </svg>
                            ${escapeHtml(doc.userName || 'Self')} (${escapeHtml(doc.userRelationship || 'Self')})
                        </span>
                    </div>

                    <div class="doc-card-org">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <rect x="2" y="7" width="20" height="14" rx="2" ry="2"></rect>
                            <path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"></path>
                        </svg>
                        <span>${escapeHtml(doc.issuingOrganization || 'Personal Document')}</span>
                    </div>

                    <div class="doc-card-meta-list">
                        ${doc.referenceNumber ? `
                            <div class="doc-card-meta-row">
                                <span class="doc-card-meta-label">Reference No:</span>
                                <strong>${escapeHtml(doc.referenceNumber)}</strong>
                            </div>` : ''}
                        <div class="doc-card-meta-row">
                            <span class="doc-card-meta-label">Expiry Date:</span>
                            <span>${doc.expiryDate ? formatDate(doc.expiryDate) : '<em style="color:var(--text-muted);">None</em>'}</span>
                        </div>
                        ${doc.daysRemaining !== null ? `
                            <div class="doc-card-meta-row">
                                <span class="doc-card-meta-label">Validity:</span>
                                <span>${doc.daysRemaining < 0 ? `<strong style="color:#ef4444;">Expired ${Math.abs(doc.daysRemaining)} days ago</strong>` : `${doc.daysRemaining} days remaining`}</span>
                            </div>` : ''}
                    </div>
                </div>

                <div class="doc-card-footer">
                    <button class="btn btn-secondary btn-sm" onclick="window.viewDoc(${doc.id})">
                        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                            <circle cx="12" cy="12" r="3"></circle>
                        </svg>
                        Details
                    </button>
                    <div style="display: flex; gap: 6px;">
                        <button class="btn btn-icon" onclick="window.editDoc(${doc.id})" title="Edit Document">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                            </svg>
                        </button>
                        <button class="btn btn-icon danger" onclick="window.promptDeleteDoc(${doc.id}, '${escapeQuote(doc.documentName)}')" title="Delete Document">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <polyline points="3 6 5 6 21 6"></polyline>
                                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                            </svg>
                        </button>
                    </div>
                </div>
            `;
            el.documentsGridContainer.appendChild(card);
        });

        // 2. Table Rows
        el.allDocsTableBody.innerHTML = '';
        docs.forEach(doc => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>
                    <div class="doc-name-cell">
                        ${renderThumbnailHtml(doc)}
                        <div>
                            <span class="doc-text-title">${escapeHtml(doc.documentName)}</span>
                            <span class="doc-text-subtitle">${escapeHtml(doc.issuingOrganization || '—')}</span>
                        </div>
                    </div>
                </td>
                <td><span class="doc-owner-badge">${escapeHtml(doc.userName || 'Self')}</span></td>
                <td>${getCategoryBadgeHtml(doc.category)}</td>
                <td>${doc.referenceNumber ? '#' + escapeHtml(doc.referenceNumber) : '—'}</td>
                <td>${doc.issueDate ? formatDate(doc.issueDate) : '—'}</td>
                <td>${doc.expiryDate ? formatDate(doc.expiryDate) : '<span style="color:var(--text-muted);">No Expiry</span>'}</td>
                <td>${getStatusBadgeHtml(doc)}</td>
                <td>
                    <div style="display: flex; gap: 6px;">
                        <button class="btn btn-icon" onclick="window.viewDoc(${doc.id})" title="View Details">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                                <circle cx="12" cy="12" r="3"></circle>
                            </svg>
                        </button>
                        <button class="btn btn-icon" onclick="window.editDoc(${doc.id})" title="Edit">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                            </svg>
                        </button>
                        <button class="btn btn-icon danger" onclick="window.promptDeleteDoc(${doc.id}, '${escapeQuote(doc.documentName)}')" title="Delete">
                            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                                <polyline points="3 6 5 6 21 6"></polyline>
                                <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                            </svg>
                        </button>
                    </div>
                </td>
            `;
            el.allDocsTableBody.appendChild(row);
        });
    }

    /* ------------------------------------------------------------------
       ADD / EDIT FORM & FILE UPLOAD
       ------------------------------------------------------------------ */
    function setupFormHandling() {
        el.noExpiryCheckbox.addEventListener('change', (e) => {
            if (e.target.checked) {
                el.docExpiryDateInput.value = '';
                el.docExpiryDateInput.disabled = true;
            } else {
                el.docExpiryDateInput.disabled = false;
            }
        });

        el.btnCancelForm.addEventListener('click', () => {
            switchView('documents');
        });

        el.documentForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (!validateDocumentForm()) return;

            const submitBtn = el.documentForm.querySelector('button[type="submit"]');
            submitBtn.disabled = true;
            el.formSubmitText.textContent = state.isEditing ? 'Updating...' : 'Saving...';

            const formData = new FormData();
            formData.append('documentName', el.docNameInput.value.trim());
            formData.append('category', el.docCategorySelect.value);

            // User ownership (Admin can assign, Member defaults to self)
            if (state.currentUser && state.currentUser.admin && el.docMemberSelect.value) {
                formData.append('userId', el.docMemberSelect.value);
            }

            if (el.docOrgInput.value.trim()) {
                formData.append('issuingOrganization', el.docOrgInput.value.trim());
            }
            if (el.docRefInput.value.trim()) {
                formData.append('referenceNumber', el.docRefInput.value.trim());
            }
            if (el.docIssueDateInput.value) {
                formData.append('issueDate', el.docIssueDateInput.value);
            }
            if (!el.noExpiryCheckbox.checked && el.docExpiryDateInput.value) {
                formData.append('expiryDate', el.docExpiryDateInput.value);
            }
            if (el.docNotesInput.value.trim()) {
                formData.append('notes', el.docNotesInput.value.trim());
            }

            if (state.selectedFile) {
                formData.append('image', state.selectedFile);
            }

            if (state.removeExistingImage) {
                formData.append('removeImage', 'true');
            }

            try {
                if (state.isEditing) {
                    const id = el.docIdField.value;
                    const res = await API.updateDocument(id, formData);
                    showToast('Document Updated', `'${res.data.documentName}' was updated successfully.`, 'success');
                } else {
                    const res = await API.createDocument(formData);
                    showToast('Document Saved', `'${res.data.documentName}' was saved to the vault.`, 'success');
                }

                resetDocumentForm();
                switchView('documents');
                loadDashboard();
                loadFamilyMembers();
            } catch (error) {
                showToast('Save Failed', error.message, 'error');
            } finally {
                submitBtn.disabled = false;
                el.formSubmitText.textContent = state.isEditing ? 'Save Changes' : 'Save Document';
            }
        });
    }

    function validateDocumentForm() {
        let isValid = true;
        clearValidationErrors();

        const name = el.docNameInput.value.trim();
        const category = el.docCategorySelect.value;
        const issueDate = el.docIssueDateInput.value;
        const expiryDate = el.docExpiryDateInput.value;
        const isNoExpiry = el.noExpiryCheckbox.checked;

        if (!name) {
            setFieldError(el.docNameInput, 'docNameFeedback', 'Document name is required');
            isValid = false;
        }

        if (!category) {
            setFieldError(el.docCategorySelect, 'docCategoryFeedback', 'Please select a document category');
            isValid = false;
        }

        if (issueDate && expiryDate && !isNoExpiry) {
            if (new Date(issueDate) > new Date(expiryDate)) {
                setFieldError(el.docIssueDateInput, 'docIssueDateFeedback', 'Issue date cannot be after expiry date');
                setFieldError(el.docExpiryDateInput, 'docExpiryDateFeedback', 'Expiry date cannot be before issue date');
                isValid = false;
            }
        }

        return isValid;
    }

    function setFieldError(inputEl, feedbackId, message) {
        inputEl.classList.add('is-invalid');
        const feedbackEl = document.getElementById(feedbackId);
        if (feedbackEl) feedbackEl.textContent = message;
    }

    function clearValidationErrors() {
        document.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
        document.querySelectorAll('.invalid-feedback').forEach(el => el.textContent = '');
    }

    function setupFileUpload() {
        const dropzone = el.uploadDropzone;
        const fileInput = el.fileUploadInput;

        dropzone.addEventListener('click', () => fileInput.click());

        dropzone.addEventListener('dragover', (e) => {
            e.preventDefault();
            dropzone.classList.add('dragover');
        });

        dropzone.addEventListener('dragleave', () => {
            dropzone.classList.remove('dragover');
        });

        dropzone.addEventListener('drop', (e) => {
            e.preventDefault();
            dropzone.classList.remove('dragover');
            if (e.dataTransfer.files.length > 0) {
                handleFileSelection(e.dataTransfer.files[0]);
            }
        });

        fileInput.addEventListener('change', () => {
            if (fileInput.files.length > 0) {
                handleFileSelection(fileInput.files[0]);
            }
        });

        el.btnRemoveUploadedImage.addEventListener('click', () => {
            clearSelectedFile();
            state.removeExistingImage = true;
        });
    }

    function handleFileSelection(file) {
        const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png'];
        const maxBytes = 10 * 1024 * 1024; // 10MB

        if (!allowedTypes.includes(file.type.toLowerCase())) {
            showToast('Invalid File Type', 'Please upload a PNG, JPG, or JPEG image.', 'error');
            return;
        }

        if (file.size > maxBytes) {
            showToast('File Too Large', 'Maximum image upload size is 10MB.', 'error');
            return;
        }

        state.selectedFile = file;
        state.removeExistingImage = false;

        el.previewFileName.textContent = file.name;
        el.previewFileSize.textContent = formatBytes(file.size);

        const reader = new FileReader();
        reader.onload = (e) => {
            el.previewImageEl.src = e.target.result;
            el.uploadPreviewBox.style.display = 'flex';
        };
        reader.readAsDataURL(file);
    }

    function clearSelectedFile() {
        state.selectedFile = null;
        el.fileUploadInput.value = '';
        el.previewImageEl.src = '';
        el.uploadPreviewBox.style.display = 'none';
    }

    function resetAndOpenAddForm() {
        resetDocumentForm();
        state.isEditing = false;
        el.formSectionTitle.textContent = 'Add New Document';
        el.formSubmitText.textContent = 'Save Document';

        if (state.currentUser && state.currentUser.admin) {
            el.docMemberGroup.style.display = 'flex';
            el.docMemberSelect.value = state.currentUser.id;
        } else {
            el.docMemberGroup.style.display = 'none';
        }

        switchView('add-document');
    }

    function resetDocumentForm() {
        el.documentForm.reset();
        el.docIdField.value = '';
        el.docExpiryDateInput.disabled = false;
        clearSelectedFile();
        clearValidationErrors();
        state.isEditing = false;
        state.removeExistingImage = false;
    }

    /* ------------------------------------------------------------------
       DOCUMENT ACTIONS (VIEW, EDIT, DELETE)
       ------------------------------------------------------------------ */
    window.viewDoc = async function(id) {
        try {
            const res = await API.getDocumentById(id);
            if (res.success && res.data) {
                const doc = res.data;
                state.currentDocDetail = doc;

                el.detailDocTitle.textContent = doc.documentName;
                el.detailOwnerCell.innerHTML = `<span class="doc-owner-badge">${escapeHtml(doc.userName || 'Self')} (${escapeHtml(doc.userRelationship || 'Self')})</span>`;
                el.detailStatusCell.innerHTML = getStatusBadgeHtml(doc);
                el.detailCategoryCell.innerHTML = getCategoryBadgeHtml(doc.category);
                el.detailOrgCell.textContent = doc.issuingOrganization || '—';
                el.detailRefCell.textContent = doc.referenceNumber ? '#' + doc.referenceNumber : '—';
                el.detailIssueDateCell.textContent = doc.issueDate ? formatDate(doc.issueDate) : '—';
                el.detailExpiryDateCell.textContent = doc.expiryDate ? formatDate(doc.expiryDate) : 'Does not expire';
                el.detailDaysCell.textContent = doc.daysRemaining !== null ? 
                    (doc.daysRemaining < 0 ? `Expired ${Math.abs(doc.daysRemaining)} days ago` : `${doc.daysRemaining} days remaining`) : 'N/A';
                el.detailNotesCell.textContent = doc.notes || 'No notes provided.';
                el.detailCreatedCell.textContent = doc.createdAt ? `Record created on ${new Date(doc.createdAt).toLocaleString()}` : '';

                if (doc.imageUrl) {
                    el.detailImageEl.src = doc.imageUrl;
                    el.detailDownloadBtn.href = doc.imageUrl;
                    el.detailImageSection.style.display = 'flex';
                } else {
                    el.detailImageSection.style.display = 'none';
                }

                el.detailEditBtn.onclick = () => {
                    closeModal(el.viewDetailModal);
                    window.editDoc(doc.id);
                };
                el.detailDeleteBtn.onclick = () => {
                    closeModal(el.viewDetailModal);
                    window.promptDeleteDoc(doc.id, doc.documentName);
                };

                openModal(el.viewDetailModal);
            }
        } catch (error) {
            showToast('Failed to load document', error.message, 'error');
        }
    };

    window.editDoc = async function(id) {
        try {
            const res = await API.getDocumentById(id);
            if (res.success && res.data) {
                const doc = res.data;
                state.isEditing = true;
                state.removeExistingImage = false;

                el.formSectionTitle.textContent = `Edit Document — ${doc.documentName}`;
                el.formSubmitText.textContent = 'Save Changes';

                el.docIdField.value = doc.id;
                el.docNameInput.value = doc.documentName || '';
                el.docCategorySelect.value = doc.category || '';
                el.docOrgInput.value = doc.issuingOrganization || '';
                el.docRefInput.value = doc.referenceNumber || '';
                el.docIssueDateInput.value = doc.issueDate || '';

                if (state.currentUser && state.currentUser.admin) {
                    el.docMemberGroup.style.display = 'flex';
                    if (doc.userId) {
                        el.docMemberSelect.value = doc.userId;
                    }
                } else {
                    el.docMemberGroup.style.display = 'none';
                }

                if (doc.expiryDate) {
                    el.docExpiryDateInput.value = doc.expiryDate;
                    el.docExpiryDateInput.disabled = false;
                    el.noExpiryCheckbox.checked = false;
                } else {
                    el.docExpiryDateInput.value = '';
                    el.docExpiryDateInput.disabled = true;
                    el.noExpiryCheckbox.checked = true;
                }

                el.docNotesInput.value = doc.notes || '';

                if (doc.imageUrl) {
                    el.previewImageEl.src = doc.imageUrl;
                    el.previewFileName.textContent = doc.fileName || 'Attached Scan';
                    el.previewFileSize.textContent = doc.fileSize ? formatBytes(doc.fileSize) : 'Saved on server';
                    el.uploadPreviewBox.style.display = 'flex';
                } else {
                    clearSelectedFile();
                }

                switchView('add-document');
            }
        } catch (error) {
            showToast('Error loading record', error.message, 'error');
        }
    };

    window.promptDeleteDoc = function(id, docName) {
        state.deleteTargetId = id;
        el.deleteTargetDocName.textContent = `'${docName}'`;
        openModal(el.deleteConfirmModal);
    };

    function setupModals() {
        document.querySelectorAll('[data-close-modal]').forEach(btn => {
            btn.addEventListener('click', () => {
                const modalId = btn.getAttribute('data-close-modal');
                const modal = document.getElementById(modalId);
                if (modal) closeModal(modal);
            });
        });

        [
            el.viewDetailModal,
            el.deleteConfirmModal,
            el.addMemberModal,
            el.editMemberModal,
            el.premiumUpgradeModal,
            el.calendarDateModal,
            el.permanentDeleteConfirmModal,
            el.removeMemberModal
        ].forEach(modal => {
            if (modal) {
                modal.addEventListener('click', (e) => {
                    if (e.target === modal) closeModal(modal);
                });
            }
        });

        if (el.btnUpgradePremium) {
            el.btnUpgradePremium.addEventListener('click', () => {
                showToast('DocuSafe Premium', 'Subscription simulated: Unlimited family profiles activated!', 'success');
                closeModal(el.premiumUpgradeModal);
            });
        }

        // Soft Delete (Move to Trash)
        if (el.btnConfirmDelete) {
            el.btnConfirmDelete.addEventListener('click', async () => {
                if (!state.deleteTargetId) return;

                const targetId = state.deleteTargetId;
                el.btnConfirmDelete.disabled = true;
                el.btnConfirmDelete.textContent = 'Moving to Trash...';

                try {
                    await API.deleteDocument(targetId);
                    showToast('Moved to Trash', 'Document moved to Recycle Bin safely.', 'success');
                    closeModal(el.deleteConfirmModal);
                    closeModal(el.viewDetailModal);
                    loadDashboard();
                    loadFamilyMembers();
                    if (state.currentView === 'documents') {
                        loadDocuments();
                    }
                    if (state.currentView === 'trash') {
                        loadTrashDocuments();
                    }
                } catch (error) {
                    showToast('Delete Failed', error.message, 'error');
                } finally {
                    el.btnConfirmDelete.disabled = false;
                    el.btnConfirmDelete.textContent = 'Move to Trash';
                    state.deleteTargetId = null;
                }
            });
        }

        // Permanent Delete
        if (el.btnConfirmPermanentDelete) {
            el.btnConfirmPermanentDelete.addEventListener('click', async () => {
                if (!state.permanentDeleteTargetId) return;

                const targetId = state.permanentDeleteTargetId;
                el.btnConfirmPermanentDelete.disabled = true;
                el.btnConfirmPermanentDelete.textContent = 'Deleting Permanently...';

                try {
                    await API.permanentlyDeleteDocument(targetId);
                    showToast('Permanently Erased', 'Document record and storage file have been deleted.', 'success');
                    closeModal(el.permanentDeleteConfirmModal);
                    loadTrashDocuments();
                    loadDashboard();
                    loadFamilyMembers();
                    if (state.currentView === 'documents') {
                        loadDocuments();
                    }
                } catch (error) {
                    showToast('Permanent Delete Failed', error.message, 'error');
                } finally {
                    el.btnConfirmPermanentDelete.disabled = false;
                    el.btnConfirmPermanentDelete.textContent = 'Delete Permanently';
                    state.permanentDeleteTargetId = null;
                }
            });
        }

        // Remove Family Member Confirmation
        if (el.btnConfirmRemoveMember) {
            el.btnConfirmRemoveMember.addEventListener('click', async () => {
                if (!state.removeMemberTargetId) return;

                const targetId = state.removeMemberTargetId;
                const memberName = state.removeMemberTargetName || 'Member';
                el.btnConfirmRemoveMember.disabled = true;
                el.btnConfirmRemoveMember.textContent = 'Removing...';

                try {
                    await API.deleteFamilyMember(targetId);
                    showToast('Member Removed', `'${memberName}' was removed successfully.`, 'success');
                    if (el.removeMemberModal) closeModal(el.removeMemberModal);

                    // If active vault was the removed member, switch back to Admin session
                    if (state.currentUser && state.currentUser.id === parseInt(targetId, 10)) {
                        await loadMe();
                    }

                    await loadFamilyMembers();
                    await loadDashboard();
                    if (state.currentView === 'documents') {
                        loadDocuments();
                    }
                } catch (error) {
                    showToast('Remove Failed', error.message, 'error');
                } finally {
                    el.btnConfirmRemoveMember.disabled = false;
                    el.btnConfirmRemoveMember.textContent = 'Remove Member';
                    state.removeMemberTargetId = null;
                    state.removeMemberTargetName = '';
                }
            });
        }
    }

    function openModal(modal) {
        modal.classList.add('active');
        document.body.style.overflow = 'hidden';
    }

    function closeModal(modal) {
        modal.classList.remove('active');
        document.body.style.overflow = '';
    }

    /* ------------------------------------------------------------------
       HTML FORMATTING HELPERS
       ------------------------------------------------------------------ */
    function getStatusBadgeHtml(doc) {
        const cssClass = doc.statusCssClass || 'badge-neutral';
        const label = doc.statusLabel || 'Unknown';
        return `<span class="badge ${cssClass}"><span class="badge-dot"></span>${label}</span>`;
    }

    function getCategoryBadgeHtml(category) {
        const catKey = (category || 'other').toLowerCase();
        return `<span class="cat-badge cat-${catKey}">${escapeHtml(category || 'Other')}</span>`;
    }

    function renderThumbnailHtml(doc) {
        if (doc.imageUrl) {
            return `<img src="${doc.imageUrl}" class="doc-thumb" alt="${escapeHtml(doc.documentName)}" onclick="window.viewDoc(${doc.id})">`;
        }
        return `
            <div class="doc-thumb-placeholder">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                    <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                    <polyline points="14 2 14 8 20 8"></polyline>
                </svg>
            </div>
        `;
    }

    function formatDate(dateStr) {
        if (!dateStr) return '—';
        const parts = dateStr.split('-');
        if (parts.length === 3) {
            const d = new Date(parts[0], parts[1] - 1, parts[2]);
            return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
        }
        return dateStr;
    }

    function formatBytes(bytes) {
        if (!bytes || bytes === 0) return '0 B';
        const k = 1024;
        const sizes = ['B', 'KB', 'MB', 'GB'];
        const i = Math.floor(Math.log(bytes) / Math.log(k));
        return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function escapeQuote(str) {
        if (!str) return '';
        return str.replace(/'/g, "\\'");
    }

    /* ------------------------------------------------------------------
       TOAST NOTIFICATION ENGINE
       ------------------------------------------------------------------ */
    function showToast(title, message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `toast ${type}`;

        const iconSvg = getToastIconSvg(type);

        toast.innerHTML = `
            <div class="toast-icon">${iconSvg}</div>
            <div class="toast-content">
                <div class="toast-title">${escapeHtml(title)}</div>
                <div class="toast-message">${escapeHtml(message)}</div>
            </div>
            <button class="toast-close" aria-label="Close notification">&times;</button>
        `;

        el.toastContainer.appendChild(toast);
        setTimeout(() => toast.classList.add('show'), 10);

        const closeBtn = toast.querySelector('.toast-close');
        closeBtn.addEventListener('click', () => removeToast(toast));
        setTimeout(() => removeToast(toast), 4500);
    }

    function removeToast(toast) {
        toast.classList.remove('show');
        setTimeout(() => {
            if (toast.parentElement) toast.parentElement.removeChild(toast);
        }, 300);
    }

    function getToastIconSvg(type) {
        switch (type) {
            case 'success':
                return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path>
                            <polyline points="22 4 12 14.01 9 11.01"></polyline>
                        </svg>`;
            case 'error':
                return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="15" y1="9" x2="9" y2="15"></line>
                            <line x1="9" y1="9" x2="15" y2="15"></line>
                        </svg>`;
            case 'warning':
                return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
                            <line x1="12" y1="9" x2="12" y2="13"></line>
                            <line x1="12" y1="17" x2="12.01" y2="17"></line>
                        </svg>`;
            default:
                return `<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                            <circle cx="12" cy="12" r="10"></circle>
                            <line x1="12" y1="16" x2="12" y2="12"></line>
                            <line x1="12" y1="8" x2="12.01" y2="8"></line>
                        </svg>`;
        }
    }

    /* ------------------------------------------------------------------
       THEME ENGINE & BACKGROUND CUSTOMIZATION
       ------------------------------------------------------------------ */
    function setupThemeEngine() {
        const savedTheme = localStorage.getItem('docusafe_theme') || 'theme-default';
        applyTheme(savedTheme, false);

        if (el.btnThemeToggle) {
            el.btnThemeToggle.addEventListener('click', () => {
                const current = localStorage.getItem('docusafe_theme') || 'theme-default';
                const next = (current === 'theme-dark') ? 'theme-default' : 'theme-dark';
                applyTheme(next, true);
                showToast('Theme Changed', `Switched to ${next === 'theme-dark' ? 'Dark Mode' : 'Light Mode'}.`, 'info');
            });
        }

        [el.themeCardDefault, el.themeCardWhite, el.themeCardDark].forEach(card => {
            if (card) {
                card.addEventListener('click', () => {
                    const themeId = card.getAttribute('data-theme-id');
                    if (themeId) {
                        applyTheme(themeId, true);
                        showToast('Theme Updated', `Theme set to ${themeId}. Saved to your profile.`, 'success');
                    }
                });
            }
        });
    }

    function applyTheme(themeName, persistToBackend = true) {
        state.currentTheme = themeName;
        document.documentElement.className = themeName;
        localStorage.setItem('docusafe_theme', themeName);

        updateThemeSelectionUI();

        if (persistToBackend && state.currentUser) {
            API.updateTheme(themeName).catch(err => console.warn('Failed to save theme setting to DB', err));
        }
    }

    function updateThemeSelectionUI() {
        const current = state.currentTheme || localStorage.getItem('docusafe_theme') || 'theme-default';
        document.querySelectorAll('.theme-option-card').forEach(card => {
            if (card.getAttribute('data-theme-id') === current) {
                card.classList.add('active');
            } else {
                card.classList.remove('active');
            }
        });
    }

    /* ------------------------------------------------------------------
       DOCUMENT EXPIRY CALENDAR SYSTEM
       ------------------------------------------------------------------ */
    function setupCalendarView() {
        if (el.btnCalPrev) {
            el.btnCalPrev.addEventListener('click', () => {
                state.calendarMonth--;
                if (state.calendarMonth < 0) {
                    state.calendarMonth = 11;
                    state.calendarYear--;
                }
                renderCalendarGrid();
            });
        }

        if (el.btnCalNext) {
            el.btnCalNext.addEventListener('click', () => {
                state.calendarMonth++;
                if (state.calendarMonth > 11) {
                    state.calendarMonth = 0;
                    state.calendarYear++;
                }
                renderCalendarGrid();
            });
        }

        if (el.btnCalToday) {
            el.btnCalToday.addEventListener('click', () => {
                const today = new Date();
                state.calendarMonth = today.getMonth();
                state.calendarYear = today.getFullYear();
                renderCalendarGrid();
            });
        }
    }

    async function loadCalendarView() {
        if (!state.currentUser) return;
        const memberId = state.currentUser.admin ? state.selectedMemberFilter : null;
        try {
            const res = await API.getCalendarDocuments(memberId);
            if (res.success && res.data) {
                state.calendarDocs = res.data;
            } else {
                state.calendarDocs = [];
            }
        } catch (error) {
            console.warn('Failed to load calendar documents', error);
            state.calendarDocs = [];
        }
        renderCalendarGrid();
    }

    function computeDocExpiryStatus(doc) {
        if (!doc.expiryDate) {
            return { statusClass: 'status-valid', label: 'Valid (No Expiry)' };
        }
        const expParts = doc.expiryDate.split('-');
        if (expParts.length !== 3) {
            return { statusClass: 'status-valid', label: 'Valid' };
        }
        const expDate = new Date(parseInt(expParts[0], 10), parseInt(expParts[1], 10) - 1, parseInt(expParts[2], 10));
        expDate.setHours(0, 0, 0, 0);

        const now = new Date();
        const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());

        const diffMs = expDate.getTime() - today.getTime();
        const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));

        if (diffDays < 0) {
            return { statusClass: 'status-expired', label: 'Expired' };
        } else if (diffDays <= 30) {
            return { statusClass: 'status-warning', label: 'Expiring Soon' };
        } else {
            return { statusClass: 'status-valid', label: 'Valid' };
        }
    }

    function renderCalendarGrid() {
        if (!el.calendarDaysGrid) return;
        el.calendarDaysGrid.innerHTML = '';

        const year = state.calendarYear;
        const month = state.calendarMonth;

        const monthNames = [
            'January', 'February', 'March', 'April', 'May', 'June',
            'July', 'August', 'September', 'October', 'November', 'December'
        ];
        if (el.calendarMonthTitle) {
            el.calendarMonthTitle.textContent = `${monthNames[month]} ${year}`;
        }

        // Index documents by expiry date 'YYYY-MM-DD'
        const docsByDate = {};
        state.calendarDocs.forEach(doc => {
            if (!doc.expiryDate) return;
            if (!docsByDate[doc.expiryDate]) {
                docsByDate[doc.expiryDate] = [];
            }
            docsByDate[doc.expiryDate].push(doc);
        });

        // Calendar starts on Monday (1) to Sunday (7 / 0)
        const firstDayOfMonth = new Date(year, month, 1);
        let startDayOfWeek = firstDayOfMonth.getDay(); // 0 is Sunday, 1 is Monday
        let mondayOffset = (startDayOfWeek === 0 ? 6 : startDayOfWeek - 1);

        const daysInMonth = new Date(year, month + 1, 0).getDate();
        const daysInPrevMonth = new Date(year, month, 0).getDate();

        const today = new Date();
        const isCurrentYear = today.getFullYear() === year;
        const isCurrentMonth = today.getMonth() === month;
        const todayDate = today.getDate();

        // 1. Previous month trailing padding days
        for (let i = mondayOffset - 1; i >= 0; i--) {
            const dayNum = daysInPrevMonth - i;
            const cell = document.createElement('div');
            cell.className = 'cal-day-cell other-month';
            cell.innerHTML = `
                <div class="cal-day-header">
                    <span class="cal-day-num">${dayNum}</span>
                </div>
            `;
            el.calendarDaysGrid.appendChild(cell);
        }

        // 2. Current month active days
        for (let day = 1; day <= daysInMonth; day++) {
            const cell = document.createElement('div');
            cell.className = 'cal-day-cell';
            if (isCurrentYear && isCurrentMonth && day === todayDate) {
                cell.classList.add('today');
            }

            const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
            const dayDocs = docsByDate[dateStr] || [];

            let eventsHtml = '';
            if (dayDocs.length > 0) {
                eventsHtml = '<div class="cal-events-container">';
                dayDocs.slice(0, 3).forEach(doc => {
                    const statusInfo = computeDocExpiryStatus(doc);
                    eventsHtml += `
                        <div class="cal-event-pill ${statusInfo.statusClass}" title="${escapeHtml(doc.documentName)} (${escapeHtml(doc.ownerName || 'Self')}) - ${statusInfo.label}">
                            <span>•</span>
                            <span>${escapeHtml(doc.documentName)}</span>
                        </div>
                    `;
                });
                if (dayDocs.length > 3) {
                    eventsHtml += `<div class="cal-more-docs">+${dayDocs.length - 3} more</div>`;
                }
                eventsHtml += '</div>';

                cell.style.cursor = 'pointer';
                cell.addEventListener('click', () => openCalendarDateModal(dateStr, dayDocs));
            }

            cell.innerHTML = `
                <div class="cal-day-header">
                    <span class="cal-day-num">${day}</span>
                </div>
                ${eventsHtml}
            `;

            el.calendarDaysGrid.appendChild(cell);
        }

        // 3. Next month leading padding days to complete grid
        const totalCells = mondayOffset + daysInMonth;
        const trailingDays = (totalCells % 7 === 0) ? 0 : 7 - (totalCells % 7);
        for (let day = 1; day <= trailingDays; day++) {
            const cell = document.createElement('div');
            cell.className = 'cal-day-cell other-month';
            cell.innerHTML = `
                <div class="cal-day-header">
                    <span class="cal-day-num">${day}</span>
                </div>
            `;
            el.calendarDaysGrid.appendChild(cell);
        }
    }

    function openCalendarDateModal(dateStr, docs) {
        if (!el.calendarDateModal || !el.calModalDocsList) return;
        if (el.calModalTitle) el.calModalTitle.textContent = `Documents Expiring on ${formatDate(dateStr)}`;

        el.calModalDocsList.innerHTML = '';
        docs.forEach(doc => {
            const card = document.createElement('div');
            card.className = 'cal-modal-doc-card';
            card.innerHTML = `
                <div class="cal-modal-doc-left">
                    <div class="cal-modal-doc-title">${escapeHtml(doc.documentName)}</div>
                    <div class="cal-modal-doc-meta">
                        ${getCategoryBadgeHtml(doc.category)}
                        <span>•</span>
                        <span>Owner: <strong>${escapeHtml(doc.ownerName || 'Self')}</strong></span>
                        <span>•</span>
                        ${getStatusBadgeHtml(doc)}
                    </div>
                </div>
                <button type="button" class="btn btn-secondary btn-sm cal-view-details-btn">
                    View Details
                </button>
            `;

            const viewBtn = card.querySelector('.cal-view-details-btn');
            if (viewBtn) {
                viewBtn.addEventListener('click', (e) => {
                    e.stopPropagation();
                    closeModal(el.calendarDateModal);
                    window.viewDoc(doc.id);
                });
            }

            el.calModalDocsList.appendChild(card);
        });

        openModal(el.calendarDateModal);
    }

    window.openDocumentDetailsModal = function(docId) {
        if (el.calendarDateModal) closeModal(el.calendarDateModal);
        window.viewDoc(docId);
    };
    window.viewDocument = window.openDocumentDetailsModal;

    /* ------------------------------------------------------------------
       RECYCLE BIN / TRASH SYSTEM
       ------------------------------------------------------------------ */
    function setupTrashView() {
        // Any specific event listeners for trash view if needed
    }

    async function loadTrashDocuments() {
        if (!state.currentUser) return;
        const memberId = state.currentUser.admin ? state.selectedMemberFilter : null;
        try {
            const res = await API.getTrashDocuments(memberId);
            if (res.success && res.data) {
                state.trashDocuments = res.data;
                renderTrashTable(state.trashDocuments);
            }
        } catch (error) {
            showToast('Error loading trash', error.message, 'error');
        }
    }

    function renderTrashTable(docs) {
        if (!el.trashTableBody) return;
        el.trashTableBody.innerHTML = '';
        const count = docs ? docs.length : 0;
        if (el.trashCountDisplay) el.trashCountDisplay.textContent = `${count} Document${count === 1 ? '' : 's'} in Trash`;
        if (el.sidebarBadgeTrash) el.sidebarBadgeTrash.textContent = count;

        if (!docs || docs.length === 0) {
            if (el.trashEmptyState) el.trashEmptyState.style.display = 'block';
            if (el.trashTable) el.trashTable.style.display = 'none';
            return;
        }

        if (el.trashEmptyState) el.trashEmptyState.style.display = 'none';
        if (el.trashTable) el.trashTable.style.display = 'table';

        docs.forEach(doc => {
            const tr = document.createElement('tr');
            const deletedAtFormatted = doc.deletedAt ? formatDateTime(doc.deletedAt) : 'Recently';
            tr.innerHTML = `
                <td>
                    <div class="table-doc-info">
                        ${renderThumbnailHtml(doc)}
                        <div>
                            <div class="table-doc-name">${escapeHtml(doc.documentName)}</div>
                            <div class="table-doc-org">${escapeHtml(doc.issuingOrganization || '—')}</div>
                        </div>
                    </div>
                </td>
                <td>
                    <span class="user-chip-mini">
                        <span class="user-chip-dot"></span>
                        ${escapeHtml(doc.ownerName || 'Self')}
                    </span>
                </td>
                <td>${getCategoryBadgeHtml(doc.category)}</td>
                <td>${formatDate(doc.expiryDate)}</td>
                <td style="font-size: 0.82rem; color: var(--text-muted);">${deletedAtFormatted}</td>
                <td style="text-align: right; white-space: nowrap;">
                    <button class="btn-restore-action" onclick="window.restoreDoc(${doc.id}, '${escapeQuote(doc.documentName)}')">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="1 4 1 10 7 10"></polyline><path d="M3.51 15a9 9 0 1 0 2.13-9.36L1 10"></path></svg>
                        Restore
                    </button>
                    <button class="btn-permanent-delete-action" style="margin-left: 6px;" onclick="window.promptPermanentDeleteDoc(${doc.id}, '${escapeQuote(doc.documentName)}')">
                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
                        Delete Permanently
                    </button>
                </td>
            `;
            el.trashTableBody.appendChild(tr);
        });
    }

    window.restoreDoc = async function(id, docName) {
        try {
            await API.restoreDocument(id);
            showToast('Restored', `'${docName}' was restored to active documents.`, 'success');
            loadTrashDocuments();
            loadDashboard();
            if (state.currentView === 'documents') loadDocuments();
            if (state.currentView === 'calendar') loadCalendarView();
        } catch (error) {
            showToast('Restore Failed', error.message, 'error');
        }
    };

    window.promptPermanentDeleteDoc = function(id, docName) {
        state.permanentDeleteTargetId = id;
        if (el.permDeleteDocName) el.permDeleteDocName.textContent = `'${docName}'`;
        openModal(el.permanentDeleteConfirmModal);
    };

    /* ------------------------------------------------------------------
       EXPIRING SOON HOVER ALERTS & POPOVER
       ------------------------------------------------------------------ */
    function setupExpiringHoverAlerts() {
        const triggers = [el.navExpiring, el.cardExpiringDocs].filter(Boolean);

        triggers.forEach(trigger => {
            trigger.addEventListener('mouseenter', () => {
                clearTimeout(state.hoverTimer);
                state.hoverTimer = setTimeout(() => {
                    showExpiringHoverPopover(trigger);
                }, 150);
            });

            trigger.addEventListener('mouseleave', () => {
                clearTimeout(state.hoverTimer);
                state.hoverTimer = setTimeout(() => {
                    hideExpiringHoverPopover();
                }, 250);
            });
        });

        if (el.expiringHoverPopover) {
            el.expiringHoverPopover.addEventListener('mouseenter', () => {
                clearTimeout(state.hoverTimer);
            });
            el.expiringHoverPopover.addEventListener('mouseleave', () => {
                clearTimeout(state.hoverTimer);
                state.hoverTimer = setTimeout(() => {
                    hideExpiringHoverPopover();
                }, 250);
            });
        }
    }

    function showExpiringHoverPopover(targetEl) {
        if (!el.expiringHoverPopover || !el.expiringHoverPopoverBody) return;
        const expiringDocs = (state.dashboardSummary && state.dashboardSummary.expiringSoonList) 
            ? state.dashboardSummary.expiringSoonList 
            : [];

        if (expiringDocs.length === 0) return;

        el.expiringHoverPopoverBody.innerHTML = '';
        expiringDocs.slice(0, 5).forEach(doc => {
            const days = doc.daysRemaining !== null ? doc.daysRemaining : 0;
            const daysText = days <= 0 ? 'Expires today' : (days === 1 ? 'Expires in 1 day' : `Expires in ${days} days`);
            const item = document.createElement('div');
            item.className = 'expiring-popover-item';
            item.innerHTML = `
                <div class="expiring-item-info">
                    <div class="expiring-item-title">${escapeHtml(doc.documentName)}</div>
                    <div class="expiring-item-owner">${escapeHtml(doc.ownerName || 'Self')} • ${escapeHtml(doc.category)}</div>
                </div>
                <span class="expiring-item-countdown">${daysText}</span>
            `;
            item.addEventListener('click', () => {
                hideExpiringHoverPopover();
                window.viewDoc(doc.id);
            });
            el.expiringHoverPopoverBody.appendChild(item);
        });

        const rect = targetEl.getBoundingClientRect();
        let top = rect.top;
        let left = rect.right + 12;

        // If overflowing right window boundary, position to the left of the element
        if (left + 330 > window.innerWidth) {
            left = rect.left - 330;
        }
        if (left < 10) left = 10;

        // If overflowing bottom window boundary
        if (top + 280 > window.innerHeight) {
            top = Math.max(10, window.innerHeight - 290);
        }

        el.expiringHoverPopover.style.top = `${top}px`;
        el.expiringHoverPopover.style.left = `${left}px`;
        el.expiringHoverPopover.style.display = 'block';
        setTimeout(() => el.expiringHoverPopover.classList.add('show'), 10);
    }

    function hideExpiringHoverPopover() {
        if (!el.expiringHoverPopover) return;
        el.expiringHoverPopover.classList.remove('show');
        setTimeout(() => {
            if (!el.expiringHoverPopover.classList.contains('show')) {
                el.expiringHoverPopover.style.display = 'none';
            }
        }, 200);
    }

    function formatDateTime(dateTimeStr) {
        if (!dateTimeStr) return '—';
        try {
            const d = new Date(dateTimeStr);
            if (isNaN(d.getTime())) return dateTimeStr;
            return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
        } catch {
            return dateTimeStr;
        }
    }
});
