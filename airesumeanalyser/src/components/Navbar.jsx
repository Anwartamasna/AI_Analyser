import React, { useState, useEffect } from 'react';
import { Menu, X, FileText, CheckCircle, BarChart2, User, LogOut } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { Link, useNavigate } from 'react-router-dom';

const Navbar = () => {
    const [isScrolled, setIsScrolled] = useState(false);
    const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    useEffect(() => {
        const handleScroll = () => {
            if (window.scrollY > 10) {
                setIsScrolled(true);
            } else {
                setIsScrolled(false);
            }
        };

        window.addEventListener('scroll', handleScroll);
        return () => window.removeEventListener('scroll', handleScroll);
    }, []);

    const handleLogout = () => {
        logout();
        navigate('/login');
    };

    const navLinks = [
        { name: 'Home', href: '/' },
        { name: 'Features', href: '/#features' },
    ];

    return (
        <nav
            className={`fixed top-0 left-0 w-full z-50 transition-all duration-300 ${isScrolled
                    ? 'bg-white/80 backdrop-blur-md shadow-lg py-3'
                    : 'bg-transparent py-5'
                }`}
        >
            <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                <div className="flex justify-between items-center">
                    {/* Logo */}
                    <Link to="/" className="flex items-center space-x-2 cursor-pointer">
                        <div className="bg-indigo-600 p-2 rounded-lg text-white">
                            <FileText size={24} />
                        </div>
                        <span className={`text-2xl font-bold tracking-tight ${isScrolled ? 'text-gray-900' : 'text-indigo-900'}`}>
                            Resume<span className="text-indigo-600">AI</span>
                        </span>
                    </Link>

                    {/* Desktop Navigation */}
                    <div className="hidden md:flex items-center space-x-8">
                        {navLinks.map((link) => (
                            <Link
                                key={link.name}
                                to={link.href}
                                className={`text-sm font-medium transition-colors hover:text-indigo-600 ${isScrolled ? 'text-gray-700' : 'text-gray-800'
                                    }`}
                            >
                                {link.name}
                            </Link>
                        ))}

                        {user ? (
                            <div className="flex items-center space-x-4">
                                <Link to="/dashboard" className="text-gray-700 hover:text-indigo-600 font-medium text-sm flex items-center">
                                    <User size={18} className="mr-1" /> Dashboard
                                </Link>
                                <button onClick={handleLogout} className="bg-indigo-50 text-indigo-700 px-4 py-2 rounded-lg text-sm font-medium hover:bg-indigo-100 transition flex items-center">
                                    <LogOut size={16} className="mr-2" /> Logout
                                </button>
                            </div>
                        ) : (
                            <div className="flex items-center space-x-4">
                                <Link to="/login" className="text-gray-700 hover:text-indigo-600 font-medium text-sm">Login</Link>
                                <Link to="/register" className="bg-indigo-600 text-white px-5 py-2.5 rounded-full text-sm font-medium hover:bg-indigo-700 transition-all shadow-md hover:shadow-lg transform hover:-translate-y-0.5">
                                    Sign Up
                                </Link>
                            </div>
                        )}
                    </div>

                    {/* Mobile Menu Button */}
                    <div className="md:hidden">
                        <button
                            onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
                            className={`p-2 rounded-md ${isScrolled ? 'text-gray-900' : 'text-indigo-900'}`}
                        >
                            {mobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
                        </button>
                    </div>
                </div>
            </div>

            {/* Mobile Menu Dropdown */}
            {mobileMenuOpen && (
                <div className="md:hidden absolute top-full left-0 w-full bg-white shadow-xl border-t border-gray-100 py-4 px-4 flex flex-col space-y-4 animate-fade-in-down">
                    {navLinks.map((link) => (
                        <Link
                            key={link.name}
                            to={link.href}
                            className="text-gray-800 font-medium py-2 hover:text-indigo-600"
                            onClick={() => setMobileMenuOpen(false)}
                        >
                            {link.name}
                        </Link>
                    ))}
                    <div className="pt-4 border-t border-gray-100">
                        {user ? (
                            <>
                                <Link to="/dashboard" className="block w-full text-left py-2 font-medium text-gray-700">Dashboard</Link>
                                <button onClick={handleLogout} className="mt-2 w-full bg-indigo-50 text-indigo-700 py-3 rounded-xl font-bold">Logout</button>
                            </>
                        ) : (
                            <>
                                <Link to="/login" className="block w-full text-center py-2 font-medium text-gray-700 mb-2">Login</Link>
                                <Link to="/register" className="block w-full bg-indigo-600 text-white text-center py-3 rounded-xl font-bold">
                                    Sign Up
                                </Link>
                            </>
                        )}
                    </div>
                </div>
            )}
        </nav>
    );
};

export default Navbar;
