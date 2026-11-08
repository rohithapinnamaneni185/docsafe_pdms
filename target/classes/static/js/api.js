/**
 * DocuSafe - Centralized API Service Module
 * Handles all REST communication with the Spring Boot backend,
 * including authentication, family member management, and user-scoped document access.
 */
const API = (() => {
    const BASE_URL = window.location.origin;
    const API_ROOT = `${BASE_URL}/api`;

    /**
     * Generic helper for fetch requests with standardized error handling and credentials.
     */
    async function request(endpoint, options = {}) {
        const url = `${API_ROOT}${endpoint}`;
        
        // Ensure cookies (HTTP session) are sent with requests
        const config = {
            ...options,
            credentials: 'same-origin'
        };

        try {
            const response = await fetch(url, config);

            if (response.status === 204) {
                return { success: true };
            }

            const data = await response.json().catch(() => null);

            if (!response.ok) {
                if (response.status === 401 && !endpoint.includes('/auth/login')) {
                    window.dispatchEvent(new CustomEvent('auth:unauthorized'));
                }
                const errorMessage = (data && data.message) ? data.message : `HTTP Error ${response.status}: ${response.statusText}`;
                const error = new Error(errorMessage);
                error.status = response.status;
                error.data = data;
                throw error;
            }

            return data;
        } catch (error) {
            if (error.name === 'TypeError' && error.message.includes('fetch')) {
                const networkError = new Error('Cannot connect to the server. Please verify the Spring Boot backend is running on port 8080.');
                networkError.isNetworkError = true;
                throw networkError;
            }
            throw error;
        }
    }

    return {
        /* ================= AUTHENTICATION APIS ================= */
        async loginRequest(email, password) {
            return await request('/auth/login-request', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
        },

        async registerRequest(fullName, email, password) {
            return await request('/auth/register-request', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ fullName, email, password })
            });
        },

        async googleLogin(email, fullName = null) {
            return await request('/auth/google-login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, fullName })
            });
        },

        async loginInitiate(identifier, password) {
            return await this.loginRequest(identifier, password);
        },

        async verifyOtp(email, otp, tempToken = null) {
            return await request('/auth/verify-otp', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, otp, tempToken })
            });
        },

        async resendOtp(email, tempToken = null) {
            return await request('/auth/resend-otp', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, tempToken })
            });
        },

        async login(email, password) {
            return await request('/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ email, password })
            });
        },

        async logout() {
            return await request('/auth/logout', {
                method: 'POST'
            });
        },

        async getMe() {
            return await request('/auth/me');
        },

        async switchVault(userId) {
            return await request(`/auth/switch-vault/${userId}`, {
                method: 'POST'
            });
        },

        /* ================= FAMILY MEMBERS APIS ================= */
        async getFamilyMembers() {
            return await request('/users/family-members');
        },

        async addFamilyMember(memberData) {
            return await request('/users/family-members', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(memberData)
            });
        },

        async updateFamilyMember(id, memberData) {
            return await request(`/users/family-members/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(memberData)
            });
        },

        async deleteFamilyMember(id) {
            return await request(`/members/${id}`, {
                method: 'DELETE'
            });
        },

        /* ================= DASHBOARD APIS ================= */
        async getDashboardSummary(memberId = null) {
            const query = memberId ? `?memberId=${memberId}` : '';
            return await request(`/dashboard/summary${query}`);
        },

        /* ================= DOCUMENT APIS ================= */
        async getDocuments({ query = '', category = '', status = '', memberId = null, sort = 'createdAt,desc' } = {}) {
            const params = new URLSearchParams();
            if (query && query.trim()) params.append('query', query.trim());
            if (category && category.trim()) params.append('category', category.trim());
            if (status && status.trim()) params.append('status', status.trim());
            if (memberId) params.append('memberId', memberId);
            if (sort && sort.trim()) params.append('sort', sort.trim());

            const queryString = params.toString() ? `?${params.toString()}` : '';
            return await request(`/documents${queryString}`);
        },

        async getDocumentById(id) {
            return await request(`/documents/${id}`);
        },

        async createDocument(formData) {
            return await request('/documents', {
                method: 'POST',
                body: formData
            });
        },

        async updateDocument(id, formData) {
            return await request(`/documents/${id}`, {
                method: 'PUT',
                body: formData
            });
        },

        async deleteDocument(id) {
            return await request(`/documents/${id}`, {
                method: 'DELETE'
            });
        },

        /* ================= RECYCLE BIN / TRASH APIS ================= */
        async getTrashDocuments(memberId = null) {
            const query = memberId ? `?memberId=${memberId}` : '';
            return await request(`/documents/trash${query}`);
        },

        async restoreDocument(id) {
            return await request(`/documents/${id}/restore`, {
                method: 'PUT'
            });
        },

        async permanentlyDeleteDocument(id) {
            return await request(`/documents/${id}/permanent`, {
                method: 'DELETE'
            });
        },

        /* ================= CALENDAR APIS ================= */
        async getCalendarDocuments(memberId = null) {
            const query = memberId ? `?memberId=${memberId}` : '';
            return await request(`/documents/calendar${query}`);
        },

        /* ================= THEME SETTINGS APIS ================= */
        async updateTheme(theme) {
            return await request('/auth/profile/theme', {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ theme })
            });
        },

        async seedSampleData() {
            return await request('/documents/seed-samples', {
                method: 'POST'
            });
        }
    };
})();
