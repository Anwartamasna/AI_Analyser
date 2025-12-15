import React, { useState, useCallback, useMemo } from 'react';
import { BrowserRouter as Router, Routes, Route, useNavigate } from 'react-router-dom';
import { Upload, FileText, Send, Loader, CheckCircle, XCircle, ArrowRight, Zap, Shield, Target, Clock } from 'lucide-react';
import Navbar from './components/Navbar';
import Footer from './components/Footer';
import { AuthProvider, useAuth } from './context/AuthContext';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import ProtectedRoute from './components/ProtectedRoute';

const API_BASE_URL = '/api/analyze';

// Home Page Component (The previous App logic)
const Home = () => {
  const [jobDescription, setJobDescription] = useState('');
  const [resumeFile, setResumeFile] = useState(null);
  const [analysisResult, setAnalysisResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const { user, token } = useAuth();
  const navigate = useNavigate();

  // Function to handle file selection
  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file && file.size > 5 * 1024 * 1024) { // 5MB limit
      alert('File size exceeds 5MB limit.');
      setResumeFile(null);
      return;
    }
    setResumeFile(file);
  };

  // Function to handle form submission and API call
  const handleSubmit = useCallback(async (e) => {
    e.preventDefault();

    if (!user) {
      // Redirect to login if not authenticated
      navigate('/login');
      return;
    }

    if (!resumeFile || !jobDescription.trim()) {
      setError('Please upload a resume and provide a job description.');
      return;
    }

    setLoading(true);
    setError(null);
    setAnalysisResult(null);

    const formData = new FormData();
    formData.append('resume', resumeFile);
    formData.append('jobDescription', jobDescription);

    try {
      // Exponential backoff mechanism for fetch calls
      const fetchWithRetry = async (url, options, maxRetries = 3) => {
        for (let attempt = 0; attempt < maxRetries; attempt++) {
          try {
            const response = await fetch(url, options);
            if (response.status === 401 || response.status === 403) {
              throw new Error("Authentication failed. Please login again.");
            }
            if (!response.ok) {
              // Throw error for 4xx or 5xx status codes
              let errorMessage = `HTTP error! Status: ${response.status}`;
              try {
                const errorBody = await response.json();
                if (errorBody.error) errorMessage = errorBody.error;
              } catch (e) {
                // Response was not JSON, use default message
                console.warn('Could not parse error response JSON:', e);
              }
              throw new Error(errorMessage);
            }
            return response;
          } catch (err) {
            if (attempt < maxRetries - 1) {
              const delay = Math.pow(2, attempt) * 1000;
              await new Promise(resolve => setTimeout(resolve, delay));
            } else {
              throw err;
            }
          }
        }
      };

      const response = await fetchWithRetry(API_BASE_URL, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${token}` // Send JWT token
        },
        body: formData,
      });

      const parsedResult = await response.json();

      // Check if the analysis is still pending (timeout on backend)
      if (parsedResult.status === 'PENDING_TIMEOUT') {
        setError(null);
        setAnalysisResult({
          ...parsedResult,
          isPending: true
        });
      } else {
        setAnalysisResult(parsedResult);
      }

    } catch (err) {
      console.error('Analysis failed:', err.message);
      // Provide more helpful error messages
      if (err.message.includes('timeout') || err.message.includes('Failed to fetch')) {
        setError('Analysis is taking longer than expected. The AI model may still be processing your resume. Please check your Dashboard in a few minutes to see the results.');
      }
    } finally {
      setLoading(false);
    }
  }, [resumeFile, jobDescription, user, token, navigate]);

  // Determine styles for the score and suitability icon
  const scoreStyles = useMemo(() => {
    if (!analysisResult) return {};
    const score = analysisResult.suitability_score;
    const isSuitable = analysisResult.is_suitable;

    if (score >= 80 && isSuitable) {
      return { color: 'text-green-600', bg: 'bg-green-50', border: 'border-green-200', icon: CheckCircle };
    } else if (score >= 50) {
      return { color: 'text-yellow-600', bg: 'bg-yellow-50', border: 'border-yellow-200', icon: CheckCircle };
    } else {
      return { color: 'text-red-600', bg: 'bg-red-50', border: 'border-red-200', icon: XCircle };
    }
  }, [analysisResult]);


  // Component to render the analysis results
  const AnalysisDisplay = ({ result, styles }) => (
    <div id="results-section" className="mt-12 p-1 bg-gradient-to-br from-indigo-500 via-purple-500 to-pink-500 rounded-2xl shadow-2xl w-full max-w-4xl mx-auto transform transition-all animate-fade-in-up">
      <div className="bg-white rounded-xl p-8 md:p-10">
        <div className="flex items-center justify-between mb-8 border-b border-gray-100 pb-6">
          <h2 className="text-3xl font-bold text-gray-900 flex items-center">
            <styles.icon className={`w-8 h-8 mr-3 ${styles.color}`} />
            Analysis Results
          </h2>
          <div className="text-sm text-gray-500 font-medium bg-gray-100 px-3 py-1 rounded-full">
            AI-Powered Assessment
          </div>
        </div>

        {/* Score Card */}
        <div className={`p-6 rounded-2xl mb-8 flex flex-col md:flex-row items-center justify-between ${styles.bg} border ${styles.border}`}>
          <div className="flex items-center mb-4 md:mb-0">
            <div className={`w-20 h-20 rounded-full flex items-center justify-center text-3xl font-bold bg-white shadow-sm border-4 ${styles.border} ${styles.color} mr-6`}>
              {result.suitability_score}
            </div>
            <div>
              <p className="text-sm uppercase tracking-wide font-semibold text-gray-500 mb-1">Match Score</p>
              <h3 className={`text-2xl font-bold ${styles.color}`}>
                {result.is_suitable ? 'Great Match' : 'Review Needed'}
              </h3>
            </div>
          </div>
          <div className="text-center md:text-right">
            <div className="inline-flex items-center px-4 py-2 rounded-lg bg-white shadow-sm font-semibold text-gray-700">
              {result.is_suitable ? 'Highly Recommended' : 'Requirements Gap'}
            </div>
          </div>
        </div>

        {/* Detailed Analysis */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
          <div className="bg-gray-50 p-6 rounded-xl border border-gray-100">
            <h3 className="text-lg font-bold text-gray-800 mb-4 flex items-center">
              <span className="w-2 h-8 bg-green-500 rounded-full mr-3"></span>
              Matched Skills
            </h3>
            <ul className="space-y-3">
              {result.key_strengths?.map((item, index) => (
                <li key={index} className="flex items-start text-gray-600">
                  <span className="text-green-500 mr-2 mt-1"><CheckCircle size={16} /></span>
                  <span className="leading-relaxed">{item}</span>
                </li>
              ))}
            </ul>
          </div>

          <div className="bg-gray-50 p-6 rounded-xl border border-gray-100">
            <h3 className="text-lg font-bold text-gray-800 mb-4 flex items-center">
              <span className="w-2 h-8 bg-red-500 rounded-full mr-3"></span>
              Missing Skills
            </h3>
            <ul className="space-y-3">
              {result.key_gaps?.map((item, index) => (
                <li key={index} className="flex items-start text-gray-600">
                  <span className="text-red-500 mr-2 mt-1"><XCircle size={16} /></span>
                  <span className="leading-relaxed">{item}</span>
                </li>
              ))}
            </ul>
          </div>
        </div>

        <div className="mt-8">
          <h3 className="text-lg font-bold text-gray-800 mb-4 flex items-center">
            <span className="w-2 h-8 bg-indigo-500 rounded-full mr-3"></span>
            AI Recommendation
          </h3>
          <div className="p-6 bg-indigo-50 border border-indigo-100 rounded-xl text-indigo-900 leading-relaxed italic relative">
            <span className="absolute top-4 left-4 text-4xl text-indigo-200 opacity-50">"</span>
            {result.recommendation}
            <span className="absolute bottom-4 right-4 text-4xl text-indigo-200 opacity-50">"</span>
          </div>
        </div>
      </div>
    </div>
  );


  return (
    <div className="min-h-screen bg-gray-50 font-sans flex flex-col">
      <script src="https://cdn.tailwindcss.com"></script>
      <Navbar />

      <main className="flex-grow pt-24 pb-20 px-4 sm:px-6 lg:px-8">

        {/* Hero / Intro Section */}
        <div className="text-center max-w-4xl mx-auto mb-16">
          <h1 className="text-5xl md:text-6xl font-extrabold text-transparent bg-clip-text bg-gradient-to-r from-indigo-600 via-purple-600 to-pink-600 mb-6 tracking-tight">
            Optimize Your Career Path
          </h1>
          <p className="text-xl text-gray-500 leading-relaxed max-w-2xl mx-auto">
            Upload your resume and the job description to get an instant, AI-powered suitability analysis.
          </p>
        </div>

        {/* Features Grid */}
        <div className="max-w-6xl mx-auto grid grid-cols-1 md:grid-cols-3 gap-8 mb-16">
          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
            <div className="w-12 h-12 bg-indigo-100 rounded-xl flex items-center justify-center text-indigo-600 mb-4">
              <Zap size={24} />
            </div>
            <h3 className="text-lg font-bold text-gray-900 mb-2">Instant Analysis</h3>
            <p className="text-gray-500">Get feedback in seconds.</p>
          </div>
          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
            <div className="w-12 h-12 bg-green-100 rounded-xl flex items-center justify-center text-green-600 mb-4">
              <Target size={24} />
            </div>
            <h3 className="text-lg font-bold text-gray-900 mb-2">Keyword Matching</h3>
            <p className="text-gray-500">Identify missing keywords and skills critical for the job role.</p>
          </div>
          <div className="bg-white p-6 rounded-2xl shadow-sm border border-gray-100 hover:shadow-md transition-shadow">
            <div className="w-12 h-12 bg-purple-100 rounded-xl flex items-center justify-center text-purple-600 mb-4">
              <Shield size={24} />
            </div>
            <h3 className="text-lg font-bold text-gray-900 mb-2">Secure & Private</h3>
            <p className="text-gray-500">Your data is processed securely and is never shared with third parties.</p>
          </div>
        </div>


        <div className="w-full max-w-4xl mx-auto bg-white shadow-xl shadow-indigo-100/50 rounded-3xl p-8 sm:p-12 border border-gray-100 relative overflow-hidden">
          {/* Decorative background blobs */}
          <div className="absolute top-0 right-0 -mr-20 -mt-20 w-64 h-64 bg-indigo-50 rounded-full blur-3xl opacity-50"></div>
          <div className="absolute bottom-0 left-0 -ml-20 -mb-20 w-64 h-64 bg-pink-50 rounded-full blur-3xl opacity-50"></div>

          <form onSubmit={handleSubmit} className="space-y-8 relative z-10">

            {!user && (
              <div className="absolute inset-0 bg-white/60 backdrop-blur-sm z-50 flex flex-col items-center justify-center p-6 text-center rounded-3xl">
                <div className="bg-white p-8 rounded-2xl shadow-xl border border-indigo-100">
                  <h3 className="text-2xl font-bold text-gray-900 mb-2">Login Required</h3>
                  <p className="text-gray-500 mb-6">Please sign in to run an analysis and save your results.</p>
                  <div className="flex space-x-4 justify-center">
                    <a href="/login" onClick={(e) => { e.preventDefault(); navigate('/login') }} className="px-6 py-2.5 bg-indigo-600 text-white rounded-xl font-semibold shadow hover:bg-indigo-700 transition">Login</a>
                    <a href="/register" onClick={(e) => { e.preventDefault(); navigate('/register') }} className="px-6 py-2.5 bg-white text-indigo-600 border border-indigo-200 rounded-xl font-semibold hover:bg-indigo-50 transition">Sign Up</a>
                  </div>
                </div>
              </div>
            )}

            {/* File Upload Section */}
            <div>
              <h3 className="text-lg font-bold text-gray-800 mb-4">1. specific your resume</h3>
              <div className={`p-8 border-2 border-dashed rounded-2xl transition-all duration-300 text-center cursor-pointer group ${resumeFile ? 'border-green-400 bg-green-50/30' : 'border-indigo-200 hover:border-indigo-500 hover:bg-indigo-50/30'}`}>
                <label htmlFor="resume-upload" className="block w-full h-full cursor-pointer">
                  <input
                    id="resume-upload"
                    type="file"
                    accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
                    onChange={handleFileChange}
                    className="sr-only"
                  />
                  <div className="flex flex-col items-center justify-center space-y-4">
                    <div className={`p-4 rounded-full transition-colors ${resumeFile ? 'bg-green-100 text-green-600' : 'bg-indigo-100 text-indigo-600 group-hover:bg-indigo-600 group-hover:text-white'}`}>
                      {resumeFile ? <CheckCircle size={32} /> : <Upload size={32} />}
                    </div>
                    <div>
                      <p className="text-lg font-medium text-gray-900">
                        {resumeFile ? resumeFile.name : 'Click to upload or drag and drop'}
                      </p>
                      <p className="text-sm text-gray-500 mt-1">
                        {resumeFile ? `${(resumeFile.size / 1024 / 1024).toFixed(2)} MB uploaded` : 'PDF, DOCX, or Images (max 5MB)'}
                      </p>
                    </div>
                  </div>
                </label>
              </div>
            </div>

            {/* Job Description Textarea */}
            <div>
              <label htmlFor="job-description" className="block text-lg font-bold text-gray-800 mb-4">
                2. Paste the Job Description
              </label>
              <div className="relative">
                <textarea
                  id="job-description"
                  rows="8"
                  value={jobDescription}
                  onChange={(e) => setJobDescription(e.target.value)}
                  placeholder="Paste the full job description here..."
                  className="w-full p-5 border border-gray-200 rounded-xl focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 transition resize-none bg-gray-50 focus:bg-white text-gray-700 placeholder-gray-400 font-mono text-sm leading-relaxed"
                  required
                />
                <div className="absolute top-4 right-4 text-gray-300 pointer-events-none">
                  <FileText size={20} />
                </div>
              </div>
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              disabled={loading || !resumeFile || !jobDescription.trim()}
              className="w-full flex justify-center items-center py-4 px-6 border border-transparent rounded-xl shadow-lg shadow-indigo-500/30 text-lg font-bold text-white bg-gradient-to-r from-indigo-600 to-purple-600 hover:from-indigo-700 hover:to-purple-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition-all transform hover:-translate-y-1 disabled:opacity-50 disabled:cursor-not-allowed disabled:transform-none"
            >
              {loading ? (
                <>
                  <Loader className="w-6 h-6 mr-2 animate-spin" />
                  Analyzing Resume...
                </>
              ) : (
                <>
                  <span className="mr-2">Run Analysis</span>
                  <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                </>
              )}
            </button>
          </form>

          {/* Error Message */}
          {error && (
            <div className={`mt-8 p-4 border-l-4 rounded-r-xl ${error.includes('taking longer')
              ? 'bg-yellow-50 border-yellow-500 text-yellow-800'
              : 'bg-red-50 border-red-500 text-red-700 animate-shake'
              }`}>
              <p className="font-bold flex items-center">
                {error.includes('taking longer')
                  ? <><Clock className="w-5 h-5 mr-2" /> Processing in Background</>
                  : <><XCircle className="w-5 h-5 mr-2" /> Analysis Error</>}
              </p>
              <p className="mt-1">{error}</p>
              {error.includes('taking longer') && (
                <a
                  href="/dashboard"
                  className="mt-3 inline-flex items-center text-indigo-600 font-semibold hover:text-indigo-800 transition"
                >
                  Go to Dashboard <ArrowRight className="w-4 h-4 ml-1" />
                </a>
              )}
            </div>
          )}

          {/* Pending Analysis Notice */}
          {analysisResult?.isPending && (
            <div className="mt-8 p-4 bg-blue-50 border-l-4 border-blue-500 text-blue-800 rounded-r-xl">
              <p className="font-bold flex items-center"><Clock className="w-5 h-5 mr-2" /> Analysis Still Processing</p>
              <p className="mt-1">Your analysis is taking longer than expected. The AI model is still processing your resume in the background.</p>
              <p className="mt-2">Please check your <a href="/dashboard" className="text-indigo-600 font-semibold hover:underline">Dashboard</a> in a few minutes to see your results.</p>
            </div>
          )}
        </div>

        {/* Analysis Results Display - only show if not pending */}
        {analysisResult && !analysisResult.isPending && (
          <AnalysisDisplay result={analysisResult} styles={scoreStyles} />
        )}
      </main>

      <Footer />
    </div>
  );
};

// Main App which provides context and routing
const App = () => {
  return (
    <Router>
      <AuthProvider>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/login" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route
            path="/dashboard"
            element={
              <ProtectedRoute>
                <Dashboard />
              </ProtectedRoute>
            }
          />
        </Routes>
      </AuthProvider>
    </Router>
  )
}

export default App;