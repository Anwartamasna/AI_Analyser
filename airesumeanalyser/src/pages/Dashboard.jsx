import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import { FileText, Plus, Clock, Download, CheckCircle, XCircle, Eye, X } from 'lucide-react';

const Dashboard = () => {
    const { user } = useAuth();
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [selectedItem, setSelectedItem] = useState(null);

    useEffect(() => {
        const fetchHistory = async () => {
            try {
                const token = localStorage.getItem('token');
                const response = await fetch('/api/profile/history', {
                    headers: {
                        'Authorization': `Bearer ${token}`
                    }
                });
                if (response.ok) {
                    const data = await response.json();
                    setHistory(data);
                }
            } catch (error) {
                console.error("Failed to fetch history", error);
            } finally {
                setLoading(false);
            }
        };

        fetchHistory();
    }, []);

    const closeModal = () => setSelectedItem(null);

    // Parse JSON strings safely
    const parseJsonField = (field) => {
        if (!field) return [];
        try {
            if (typeof field === 'string') {
                return JSON.parse(field);
            }
            return field;
        } catch {
            return field.split(',').map(s => s.trim());
        }
    };

    return (
        <div className="min-h-screen bg-gray-50 flex flex-col font-sans">
            <Navbar />
            <div className="flex-grow pt-24 px-4 sm:px-6 lg:px-8 max-w-7xl mx-auto w-full">

                <div className="flex flex-col md:flex-row justify-between items-center mb-10">
                    <div>
                        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
                        <p className="text-gray-500 mt-1">Welcome back, <span className="text-indigo-600 font-semibold">{user?.username}</span></p>
                    </div>
                    <Link
                        to="/"
                        className="mt-4 md:mt-0 flex items-center px-5 py-3 bg-indigo-600 text-white rounded-xl shadow-lg hover:bg-indigo-700 transition transform hover:-translate-y-0.5"
                    >
                        <Plus size={20} className="mr-2" />
                        New Analysis
                    </Link>
                </div>

                {/* Stats Overview */}
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
                    <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 flex items-center">
                        <div className="w-12 h-12 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center mr-4">
                            <FileText size={24} />
                        </div>
                        <div>
                            <p className="text-sm text-gray-500 font-medium">Total Analyses</p>
                            <h3 className="text-2xl font-bold text-gray-900">{history.length}</h3>
                        </div>
                    </div>
                </div>

                <h2 className="text-xl font-bold text-gray-900 mb-6 flex items-center">
                    <Clock size={20} className="mr-2 text-gray-400" />
                    Recent History
                </h2>

                {loading ? (
                    <div className="text-center py-20">
                        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-indigo-600 mx-auto"></div>
                        <p className="mt-4 text-gray-500">Loading history...</p>
                    </div>
                ) : history.length === 0 ? (
                    <div className="bg-white rounded-2xl p-12 text-center border border-gray-100 shadow-sm">
                        <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4 text-gray-400">
                            <FileText size={32} />
                        </div>
                        <h3 className="text-lg font-medium text-gray-900 mb-2">No analyses yet</h3>
                        <p className="text-gray-500 mb-6">Upload your first resume to get started!</p>
                        <Link to="/" className="text-indigo-600 font-semibold hover:text-indigo-700">Start Analysis &rarr;</Link>
                    </div>
                ) : (
                    <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                        <div className="overflow-x-auto">
                            <table className="w-full text-left border-collapse">
                                <thead>
                                    <tr className="bg-gray-50 border-b border-gray-100 text-xs uppercase tracking-wider text-gray-500 font-semibold">
                                        <th className="px-6 py-4">Date</th>
                                        <th className="px-6 py-4">Job Title</th>
                                        <th className="px-6 py-4">Score</th>
                                        <th className="px-6 py-4">Status</th>
                                        <th className="px-6 py-4 text-right">Actions</th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-100">
                                    {history.map((item) => (
                                        <tr key={item.id} className="hover:bg-gray-50 transition-colors">
                                            <td className="px-6 py-4 text-sm text-gray-600">
                                                {new Date(item.createdAt).toLocaleDateString()}
                                            </td>
                                            <td className="px-6 py-4 text-sm font-medium text-gray-900">
                                                {item.jobTitle || 'Analysis'}
                                            </td>
                                            <td className="px-6 py-4">
                                                <span className={`inline-flex items-center justify-center w-8 h-8 rounded-full text-xs font-bold ${item.suitabilityScore >= 80 ? 'bg-green-100 text-green-700' :
                                                        item.suitabilityScore >= 50 ? 'bg-yellow-100 text-yellow-700' : 'bg-red-100 text-red-700'
                                                    }`}>
                                                    {item.suitabilityScore ?? '-'}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4">
                                                {item.suitabilityScore >= 80 ? (
                                                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                                                        <CheckCircle size={12} className="mr-1" /> Recommended
                                                    </span>
                                                ) : item.suitabilityScore ? (
                                                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                                                        Review
                                                    </span>
                                                ) : (
                                                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-yellow-100 text-yellow-800">
                                                        <Clock size={12} className="mr-1" /> Processing
                                                    </span>
                                                )}
                                            </td>
                                            <td className="px-6 py-4 text-right">
                                                <div className="flex items-center justify-end space-x-3">
                                                    <button 
                                                        onClick={() => setSelectedItem(item)}
                                                        className="text-indigo-600 hover:text-indigo-900 text-sm font-medium flex items-center"
                                                    >
                                                        <Eye size={16} className="mr-1" /> Details
                                                    </button>
                                                    {item.fileUrl && (
                                                        <a href={item.fileUrl} target="_blank" rel="noopener noreferrer" className="text-gray-500 hover:text-gray-700 text-sm font-medium flex items-center">
                                                            <Download size={16} className="mr-1" /> PDF
                                                        </a>
                                                    )}
                                                </div>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                )}
            </div>

            {/* Detail Modal */}
            {selectedItem && (
                <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
                    <div className="bg-white rounded-2xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
                        <div className="p-6 border-b border-gray-100 flex justify-between items-center sticky top-0 bg-white">
                            <h2 className="text-xl font-bold text-gray-900">Analysis Details</h2>
                            <button onClick={closeModal} className="text-gray-400 hover:text-gray-600">
                                <X size={24} />
                            </button>
                        </div>
                        
                        <div className="p-6 space-y-6">
                            {/* Score Section */}
                            <div className={`p-4 rounded-xl flex items-center justify-between ${
                                selectedItem.suitabilityScore >= 80 ? 'bg-green-50 border border-green-200' :
                                selectedItem.suitabilityScore >= 50 ? 'bg-yellow-50 border border-yellow-200' : 'bg-red-50 border border-red-200'
                            }`}>
                                <div className="flex items-center">
                                    <div className={`w-16 h-16 rounded-full flex items-center justify-center text-2xl font-bold ${
                                        selectedItem.suitabilityScore >= 80 ? 'bg-green-100 text-green-700' :
                                        selectedItem.suitabilityScore >= 50 ? 'bg-yellow-100 text-yellow-700' : 'bg-red-100 text-red-700'
                                    }`}>
                                        {selectedItem.suitabilityScore ?? '?'}
                                    </div>
                                    <div className="ml-4">
                                        <p className="text-sm text-gray-500">Match Score</p>
                                        <p className="font-bold text-gray-900">
                                            {selectedItem.suitabilityScore >= 80 ? 'Great Match' : 
                                             selectedItem.suitabilityScore >= 50 ? 'Potential Match' : 'Needs Improvement'}
                                        </p>
                                    </div>
                                </div>
                                <div className="text-sm text-gray-500">
                                    {new Date(selectedItem.createdAt).toLocaleString()}
                                </div>
                            </div>

                            {/* Job Title */}
                            <div>
                                <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-2">Job Title</h3>
                                <p className="text-gray-900">{selectedItem.jobTitle || 'Not specified'}</p>
                            </div>

                            {/* Summary */}
                            {selectedItem.summary && (
                                <div>
                                    <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-2">Summary</h3>
                                    <p className="text-gray-700 bg-gray-50 p-4 rounded-lg">{selectedItem.summary}</p>
                                </div>
                            )}

                            {/* Matched Skills */}
                            {selectedItem.matchedSkills && (
                                <div>
                                    <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-2 flex items-center">
                                        <CheckCircle size={16} className="text-green-500 mr-2" /> Matched Skills
                                    </h3>
                                    <div className="flex flex-wrap gap-2">
                                        {parseJsonField(selectedItem.matchedSkills).map((skill, idx) => (
                                            <span key={idx} className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-sm">
                                                {skill}
                                            </span>
                                        ))}
                                    </div>
                                </div>
                            )}

                            {/* Missing Skills */}
                            {selectedItem.missingSkills && (
                                <div>
                                    <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-2 flex items-center">
                                        <XCircle size={16} className="text-red-500 mr-2" /> Missing Skills
                                    </h3>
                                    <div className="flex flex-wrap gap-2">
                                        {parseJsonField(selectedItem.missingSkills).map((skill, idx) => (
                                            <span key={idx} className="px-3 py-1 bg-red-100 text-red-800 rounded-full text-sm">
                                                {skill}
                                            </span>
                                        ))}
                                    </div>
                                </div>
                            )}

                            {/* Recommendation */}
                            {selectedItem.recommendation && (
                                <div>
                                    <h3 className="text-sm font-semibold text-gray-500 uppercase tracking-wide mb-2">Recommendation</h3>
                                    <p className="text-indigo-900 bg-indigo-50 p-4 rounded-lg italic">{selectedItem.recommendation}</p>
                                </div>
                            )}
                        </div>

                        <div className="p-6 border-t border-gray-100 flex justify-end space-x-3">
                            {selectedItem.fileUrl && (
                                <a 
                                    href={selectedItem.fileUrl} 
                                    target="_blank" 
                                    rel="noopener noreferrer"
                                    className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 flex items-center"
                                >
                                    <Download size={16} className="mr-2" /> Download Resume
                                </a>
                            )}
                            <button 
                                onClick={closeModal}
                                className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
                            >
                                Close
                            </button>
                        </div>
                    </div>
                </div>
            )}

            <Footer />
        </div>
    );
};

export default Dashboard;

