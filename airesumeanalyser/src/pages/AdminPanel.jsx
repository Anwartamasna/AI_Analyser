import React, { useState, useEffect, useCallback } from 'react';
import { Users, FileText, Trash2, Shield, BarChart3, Search, ChevronLeft, ChevronRight, AlertCircle, CheckCircle, XCircle, Edit } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';

const AdminPanel = () => {
    const { token } = useAuth();
    const [activeTab, setActiveTab] = useState('dashboard');
    const [stats, setStats] = useState({ totalUsers: 0, totalAnalyses: 0 });
    const [users, setUsers] = useState([]);
    const [analyses, setAnalyses] = useState([]);
    const [userPage, setUserPage] = useState(0);
    const [analysisPage, setAnalysisPage] = useState(0);
    const [userTotalPages, setUserTotalPages] = useState(0);
    const [analysisTotalPages, setAnalysisTotalPages] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [success, setSuccess] = useState(null);
    const [searchTerm, setSearchTerm] = useState('');
    const [editingUser, setEditingUser] = useState(null);

    const fetchStats = useCallback(async () => {
        try {
            const response = await fetch('/api/admin/stats', {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (response.ok) {
                const data = await response.json();
                setStats(data);
            }
        } catch (err) {
            console.error('Failed to fetch stats:', err);
        }
    }, [token]);

    const fetchUsers = useCallback(async (page = 0) => {
        setLoading(true);
        try {
            const response = await fetch(`/api/admin/users?page=${page}&size=10`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (response.ok) {
                const data = await response.json();
                setUsers(data.users);
                setUserPage(data.currentPage);
                setUserTotalPages(data.totalPages);
            }
        } catch (err) {
            setError('Failed to fetch users');
        } finally {
            setLoading(false);
        }
    }, [token]);

    const fetchAnalyses = useCallback(async (page = 0) => {
        setLoading(true);
        try {
            const response = await fetch(`/api/admin/analyses?page=${page}&size=10`, {
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (response.ok) {
                const data = await response.json();
                setAnalyses(data.analyses);
                setAnalysisPage(data.currentPage);
                setAnalysisTotalPages(data.totalPages);
            }
        } catch (err) {
            setError('Failed to fetch analyses');
        } finally {
            setLoading(false);
        }
    }, [token]);

    useEffect(() => {
        fetchStats();
        fetchUsers();
        fetchAnalyses();
    }, [fetchStats, fetchUsers, fetchAnalyses]);

    const handleDeleteUser = async (id, username) => {
        if (!confirm(`Are you sure you want to delete user "${username}"? This will also delete all their analyses.`)) return;
        
        try {
            const response = await fetch(`/api/admin/users/${id}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (response.ok) {
                setSuccess(`User "${username}" deleted successfully`);
                fetchUsers(userPage);
                fetchStats();
                setTimeout(() => setSuccess(null), 3000);
            } else {
                setError('Failed to delete user');
            }
        } catch (err) {
            setError('Failed to delete user');
        }
    };

    const handleUpdateRole = async (id, newRole) => {
        try {
            const response = await fetch(`/api/admin/users/${id}/role`, {
                method: 'PUT',
                headers: { 
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ role: newRole })
            });
            if (response.ok) {
                setSuccess('User role updated successfully');
                setEditingUser(null);
                fetchUsers(userPage);
                setTimeout(() => setSuccess(null), 3000);
            } else {
                setError('Failed to update user role');
            }
        } catch (err) {
            setError('Failed to update user role');
        }
    };

    const handleDeleteAnalysis = async (id) => {
        if (!confirm('Are you sure you want to delete this analysis?')) return;
        
        try {
            const response = await fetch(`/api/admin/analyses/${id}`, {
                method: 'DELETE',
                headers: { 'Authorization': `Bearer ${token}` }
            });
            if (response.ok) {
                setSuccess('Analysis deleted successfully');
                fetchAnalyses(analysisPage);
                fetchStats();
                setTimeout(() => setSuccess(null), 3000);
            } else {
                setError('Failed to delete analysis');
            }
        } catch (err) {
            setError('Failed to delete analysis');
        }
    };

    const filteredUsers = users.filter(user => 
        user.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
        user.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (user.fullName && user.fullName.toLowerCase().includes(searchTerm.toLowerCase()))
    );

    const filteredAnalyses = analyses.filter(analysis =>
        analysis.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (analysis.jobTitle && analysis.jobTitle.toLowerCase().includes(searchTerm.toLowerCase()))
    );

    return (
        <div className="min-h-screen bg-gray-50 font-sans flex flex-col">
            <script src="https://cdn.tailwindcss.com"></script>
            <Navbar />
            
            <main className="flex-grow pt-24 pb-20 px-4 sm:px-6 lg:px-8">
                <div className="max-w-7xl mx-auto">
                    {/* Header */}
                    <div className="mb-8">
                        <h1 className="text-3xl font-extrabold text-gray-900 flex items-center">
                            <Shield className="w-8 h-8 mr-3 text-indigo-600" />
                            Admin Panel
                        </h1>
                        <p className="text-gray-500 mt-2">Manage users and resume analyses</p>
                    </div>

                    {/* Alerts */}
                    {error && (
                        <div className="mb-6 p-4 bg-red-50 border-l-4 border-red-500 text-red-700 rounded-r-xl flex items-center">
                            <AlertCircle className="w-5 h-5 mr-2" />
                            {error}
                            <button onClick={() => setError(null)} className="ml-auto"><XCircle className="w-5 h-5" /></button>
                        </div>
                    )}
                    {success && (
                        <div className="mb-6 p-4 bg-green-50 border-l-4 border-green-500 text-green-700 rounded-r-xl flex items-center">
                            <CheckCircle className="w-5 h-5 mr-2" />
                            {success}
                        </div>
                    )}

                    {/* Tabs */}
                    <div className="mb-6 flex space-x-1 bg-gray-100 p-1 rounded-xl w-fit">
                        {[
                            { id: 'dashboard', label: 'Dashboard', icon: BarChart3 },
                            { id: 'users', label: 'Users', icon: Users },
                            { id: 'analyses', label: 'Analyses', icon: FileText }
                        ].map(tab => (
                            <button
                                key={tab.id}
                                onClick={() => setActiveTab(tab.id)}
                                className={`flex items-center px-4 py-2 rounded-lg font-medium transition-all ${
                                    activeTab === tab.id 
                                        ? 'bg-white text-indigo-600 shadow-sm' 
                                        : 'text-gray-600 hover:text-gray-900'
                                }`}
                            >
                                <tab.icon className="w-4 h-4 mr-2" />
                                {tab.label}
                            </button>
                        ))}
                    </div>

                    {/* Dashboard Tab */}
                    {activeTab === 'dashboard' && (
                        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                            <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
                                <div className="flex items-center justify-between">
                                    <div>
                                        <p className="text-sm font-medium text-gray-500 uppercase tracking-wide">Total Users</p>
                                        <p className="text-3xl font-bold text-gray-900 mt-2">{stats.totalUsers}</p>
                                    </div>
                                    <div className="w-12 h-12 bg-indigo-100 rounded-xl flex items-center justify-center text-indigo-600">
                                        <Users className="w-6 h-6" />
                                    </div>
                                </div>
                            </div>
                            <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
                                <div className="flex items-center justify-between">
                                    <div>
                                        <p className="text-sm font-medium text-gray-500 uppercase tracking-wide">Total Analyses</p>
                                        <p className="text-3xl font-bold text-gray-900 mt-2">{stats.totalAnalyses}</p>
                                    </div>
                                    <div className="w-12 h-12 bg-green-100 rounded-xl flex items-center justify-center text-green-600">
                                        <FileText className="w-6 h-6" />
                                    </div>
                                </div>
                            </div>
                            <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100">
                                <div className="flex items-center justify-between">
                                    <div>
                                        <p className="text-sm font-medium text-gray-500 uppercase tracking-wide">Avg per User</p>
                                        <p className="text-3xl font-bold text-gray-900 mt-2">
                                            {stats.totalUsers > 0 ? (stats.totalAnalyses / stats.totalUsers).toFixed(1) : 0}
                                        </p>
                                    </div>
                                    <div className="w-12 h-12 bg-purple-100 rounded-xl flex items-center justify-center text-purple-600">
                                        <BarChart3 className="w-6 h-6" />
                                    </div>
                                </div>
                            </div>
                        </div>
                    )}

                    {/* Users Tab */}
                    {activeTab === 'users' && (
                        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                            {/* Search */}
                            <div className="p-4 border-b border-gray-100">
                                <div className="relative">
                                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                                    <input
                                        type="text"
                                        placeholder="Search users..."
                                        value={searchTerm}
                                        onChange={(e) => setSearchTerm(e.target.value)}
                                        className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                                    />
                                </div>
                            </div>
                            
                            {/* Table */}
                            <div className="overflow-x-auto">
                                <table className="w-full">
                                    <thead className="bg-gray-50">
                                        <tr>
                                            <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">User</th>
                                            <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Email</th>
                                            <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Role</th>
                                            <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Analyses</th>
                                            <th className="px-6 py-3 text-right text-xs font-semibold text-gray-500 uppercase tracking-wider">Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-gray-100">
                                        {loading ? (
                                            <tr><td colSpan="5" className="px-6 py-12 text-center text-gray-500">Loading...</td></tr>
                                        ) : filteredUsers.length === 0 ? (
                                            <tr><td colSpan="5" className="px-6 py-12 text-center text-gray-500">No users found</td></tr>
                                        ) : (
                                            filteredUsers.map(user => (
                                                <tr key={user.id} className="hover:bg-gray-50 transition-colors">
                                                    <td className="px-6 py-4">
                                                        <div>
                                                            <p className="font-medium text-gray-900">{user.username}</p>
                                                            <p className="text-sm text-gray-500">{user.fullName || '-'}</p>
                                                        </div>
                                                    </td>
                                                    <td className="px-6 py-4 text-gray-600">{user.email}</td>
                                                    <td className="px-6 py-4">
                                                        {editingUser === user.id ? (
                                                            <select
                                                                defaultValue={user.role}
                                                                onChange={(e) => handleUpdateRole(user.id, e.target.value)}
                                                                onBlur={() => setEditingUser(null)}
                                                                autoFocus
                                                                className="border border-gray-200 rounded px-2 py-1 text-sm"
                                                            >
                                                                <option value="USER">USER</option>
                                                                <option value="ADMIN">ADMIN</option>
                                                            </select>
                                                        ) : (
                                                            <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                                                                user.role === 'ADMIN' 
                                                                    ? 'bg-purple-100 text-purple-800' 
                                                                    : 'bg-blue-100 text-blue-800'
                                                            }`}>
                                                                {user.role}
                                                            </span>
                                                        )}
                                                    </td>
                                                    <td className="px-6 py-4 text-gray-600">{user.analysisCount}</td>
                                                    <td className="px-6 py-4 text-right space-x-2">
                                                        <button
                                                            onClick={() => setEditingUser(user.id)}
                                                            className="p-2 text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors"
                                                            title="Edit Role"
                                                        >
                                                            <Edit className="w-4 h-4" />
                                                        </button>
                                                        <button
                                                            onClick={() => handleDeleteUser(user.id, user.username)}
                                                            className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                                                            title="Delete User"
                                                        >
                                                            <Trash2 className="w-4 h-4" />
                                                        </button>
                                                    </td>
                                                </tr>
                                            ))
                                        )}
                                    </tbody>
                                </table>
                            </div>
                            
                            {/* Pagination */}
                            {userTotalPages > 1 && (
                                <div className="px-6 py-4 border-t border-gray-100 flex items-center justify-between">
                                    <p className="text-sm text-gray-500">Page {userPage + 1} of {userTotalPages}</p>
                                    <div className="flex space-x-2">
                                        <button
                                            onClick={() => fetchUsers(userPage - 1)}
                                            disabled={userPage === 0}
                                            className="p-2 border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                                        >
                                            <ChevronLeft className="w-4 h-4" />
                                        </button>
                                        <button
                                            onClick={() => fetchUsers(userPage + 1)}
                                            disabled={userPage >= userTotalPages - 1}
                                            className="p-2 border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                                        >
                                            <ChevronRight className="w-4 h-4" />
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}

                    {/* Analyses Tab */}
                    {activeTab === 'analyses' && (
                        <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                            {/* Search */}
                            <div className="p-4 border-b border-gray-100">
                                <div className="relative">
                                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-5 h-5" />
                                    <input
                                        type="text"
                                        placeholder="Search analyses..."
                                        value={searchTerm}
                                        onChange={(e) => setSearchTerm(e.target.value)}
                                        className="w-full pl-10 pr-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                                    />
                                </div>
                            </div>
                            
                            {/* Table */}
                            <div className="overflow-x-auto">
                                <table className="w-full">
                                    <thead className="bg-gray-50">
                                        <tr>
                                            <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">ID</th>
                                            <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">User</th>
                                            <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Job Title</th>
                                            <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Score</th>
                                            <th className="px-6 py-3 text-left text-xs font-semibold text-gray-500 uppercase tracking-wider">Date</th>
                                            <th className="px-6 py-3 text-right text-xs font-semibold text-gray-500 uppercase tracking-wider">Actions</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-gray-100">
                                        {loading ? (
                                            <tr><td colSpan="6" className="px-6 py-12 text-center text-gray-500">Loading...</td></tr>
                                        ) : filteredAnalyses.length === 0 ? (
                                            <tr><td colSpan="6" className="px-6 py-12 text-center text-gray-500">No analyses found</td></tr>
                                        ) : (
                                            filteredAnalyses.map(analysis => (
                                                <tr key={analysis.id} className="hover:bg-gray-50 transition-colors">
                                                    <td className="px-6 py-4 text-gray-500">#{analysis.id}</td>
                                                    <td className="px-6 py-4 font-medium text-gray-900">{analysis.username}</td>
                                                    <td className="px-6 py-4 text-gray-600">{analysis.jobTitle || '-'}</td>
                                                    <td className="px-6 py-4">
                                                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${
                                                            analysis.suitabilityScore >= 75 
                                                                ? 'bg-green-100 text-green-800'
                                                                : analysis.suitabilityScore >= 50
                                                                    ? 'bg-yellow-100 text-yellow-800'
                                                                    : 'bg-red-100 text-red-800'
                                                        }`}>
                                                            {analysis.suitabilityScore || 'N/A'}
                                                        </span>
                                                    </td>
                                                    <td className="px-6 py-4 text-gray-500 text-sm">
                                                        {new Date(analysis.createdAt).toLocaleDateString()}
                                                    </td>
                                                    <td className="px-6 py-4 text-right">
                                                        <button
                                                            onClick={() => handleDeleteAnalysis(analysis.id)}
                                                            className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors"
                                                            title="Delete Analysis"
                                                        >
                                                            <Trash2 className="w-4 h-4" />
                                                        </button>
                                                    </td>
                                                </tr>
                                            ))
                                        )}
                                    </tbody>
                                </table>
                            </div>
                            
                            {/* Pagination */}
                            {analysisTotalPages > 1 && (
                                <div className="px-6 py-4 border-t border-gray-100 flex items-center justify-between">
                                    <p className="text-sm text-gray-500">Page {analysisPage + 1} of {analysisTotalPages}</p>
                                    <div className="flex space-x-2">
                                        <button
                                            onClick={() => fetchAnalyses(analysisPage - 1)}
                                            disabled={analysisPage === 0}
                                            className="p-2 border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                                        >
                                            <ChevronLeft className="w-4 h-4" />
                                        </button>
                                        <button
                                            onClick={() => fetchAnalyses(analysisPage + 1)}
                                            disabled={analysisPage >= analysisTotalPages - 1}
                                            className="p-2 border border-gray-200 rounded-lg hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                                        >
                                            <ChevronRight className="w-4 h-4" />
                                        </button>
                                    </div>
                                </div>
                            )}
                        </div>
                    )}
                </div>
            </main>

            <Footer />
        </div>
    );
};

export default AdminPanel;
