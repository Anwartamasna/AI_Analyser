import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { useNavigate, Link } from 'react-router-dom';
import Navbar from '../components/Navbar';
import Footer from '../components/Footer';
import { FileText, Plus, Clock, Download, CheckCircle, XCircle } from 'lucide-react';

const Dashboard = () => {
    const { user } = useAuth();
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);

    useEffect(() => {
        const fetchHistory = async () => {
            try {
                const token = localStorage.getItem('token');
                const response = await fetch('http://localhost:8080/api/profile/history', {
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

                {/* Stats Overview (Mocked for now) */}
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
                    {/* Add more stats if available from backend */}
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
                                                    {item.suitabilityScore}
                                                </span>
                                            </td>
                                            <td className="px-6 py-4">
                                                {item.suitabilityScore >= 80 ? (
                                                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-green-100 text-green-800">
                                                        <CheckCircle size={12} className="mr-1" /> Recommended
                                                    </span>
                                                ) : (
                                                    <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">
                                                        Review
                                                    </span>
                                                )}
                                            </td>
                                            <td className="px-6 py-4 text-right">
                                                {item.fileUrl && (
                                                    <a href={item.fileUrl} target="_blank" rel="noopener noreferrer" className="text-indigo-600 hover:text-indigo-900 text-sm font-medium flex items-center justify-end">
                                                        <Download size={16} className="mr-1" /> PDF
                                                    </a>
                                                )}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                )}
            </div>
            <Footer />
        </div>
    );
};

export default Dashboard;
