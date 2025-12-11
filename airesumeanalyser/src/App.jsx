import React, { useState, useCallback, useMemo } from 'react';
import { Upload, FileText, Send, Loader, CheckCircle, XCircle } from 'lucide-react';

const API_BASE_URL = 'http://localhost:8080/api/analyze';

// Main App Component
const App = () => {
  const [jobDescription, setJobDescription] = useState('');
  const [resumeFile, setResumeFile] = useState(null);
  const [analysisResult, setAnalysisResult] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

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
              throw new Error("Authentication failed. Check API Key or CORS setup.");
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
        body: formData,
      });

      const rawJsonText = await response.text();
      // The backend returns a raw JSON string from the AI, which needs to be parsed again.
      const parsedResult = JSON.parse(rawJsonText);
      setAnalysisResult(parsedResult);

    } catch (err) {
      console.error('Analysis failed:', err.message);
      setError(`Analysis Failed: ${err.message}. Please check your Spring Boot server and API key.`);
    } finally {
      setLoading(false);
    }
  }, [resumeFile, jobDescription]);

  // Determine styles for the score and suitability icon
  const scoreStyles = useMemo(() => {
    if (!analysisResult) return {};
    const score = analysisResult.suitability_score;
    const isSuitable = analysisResult.is_suitable;

    if (score >= 80 && isSuitable) {
      return { color: 'text-green-600', bg: 'bg-green-100', icon: CheckCircle };
    } else if (score >= 50) {
      return { color: 'text-yellow-600', bg: 'bg-yellow-100', icon: CheckCircle };
    } else {
      return { color: 'text-red-600', bg: 'bg-red-100', icon: XCircle };
    }
  }, [analysisResult]);


  // Component to render the analysis results
  const AnalysisDisplay = ({ result, styles }) => (
    <div className="mt-8 p-6 bg-white shadow-xl rounded-2xl w-full max-w-2xl border border-gray-100">
      <h2 className="text-3xl font-extrabold text-gray-900 mb-6 border-b pb-3 flex items-center">
        <styles.icon className={`w-8 h-8 mr-3 ${styles.color}`} />
        Suitability Assessment
      </h2>

      {/* Score Card */}
      <div className={`p-4 rounded-xl mb-6 flex items-center justify-between ${styles.bg} border-l-4 border-${styles.color.slice(5)}`}>
        <div>
          <p className="text-sm font-medium text-gray-600">Overall Suitability Score (Out of 100)</p>
          <p className={`text-5xl font-bold ${styles.color}`}>{result.suitability_score}</p>
        </div>
        <div className="text-right">
          <p className="text-lg font-semibold text-gray-800">
            {result.is_suitable ? 'Highly Recommended' : 'Further Review Needed'}
          </p>
        </div>
      </div>

      {/* Detailed Analysis */}
      <div className="space-y-6">
        <div>
          <h3 className="text-xl font-semibold text-gray-800 mb-2">Key Strengths (Matches)</h3>
          <ul className="list-disc pl-5 space-y-1 text-gray-600">
            {result.key_strengths?.map((item, index) => (
              <li key={index} className="flex items-start">
                <span className="text-green-500 mr-2">•</span>
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>

        <div>
          <h3 className="text-xl font-semibold text-gray-800 mb-2">Key Gaps (Areas to Improve)</h3>
          <ul className="list-disc pl-5 space-y-1 text-gray-600">
            {result.key_gaps?.map((item, index) => (
              <li key={index} className="flex items-start">
                <span className="text-red-500 mr-2">•</span>
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </div>

        <div>
          <h3 className="text-xl font-semibold text-gray-800 mb-2">Recommendation</h3>
          <div className="p-3 bg-indigo-50 border-l-4 border-indigo-500 text-indigo-800 rounded-lg italic">
            {result.recommendation}
          </div>
        </div>
      </div>
    </div>
  );


  return (
    <div className="min-h-screen bg-gray-50 p-4 sm:p-8 flex flex-col items-center font-sans">
      <script src="[https://cdn.tailwindcss.com](https://cdn.tailwindcss.com)"></script>
      <div className="w-full max-w-3xl bg-white shadow-2xl rounded-3xl p-6 sm:p-10">
        <h1 className="text-4xl font-extrabold text-center text-indigo-700 mb-2">AI Resume Matcher</h1>
        <p className="text-center text-gray-500 mb-8">Upload your resume and the job description for instant suitability analysis.</p>

        <form onSubmit={handleSubmit} className="space-y-6">

          {/* File Upload Section */}
          <div className="p-5 border-2 border-dashed border-indigo-300 rounded-xl hover:border-indigo-500 transition duration-300">
            <label htmlFor="resume-upload" className="block text-lg font-medium text-gray-700 mb-2 cursor-pointer">
              1. Upload Resume (PDF, DOCX, PNG)
            </label>
            <input
              id="resume-upload"
              type="file"
              accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
              onChange={handleFileChange}
              className="sr-only" // Hide the default input
            />
            <div className="flex items-center justify-between p-3 bg-indigo-50 rounded-lg cursor-pointer" onClick={() => document.getElementById('resume-upload').click()}>
              <div className="flex items-center">
                <Upload className="w-5 h-5 text-indigo-600 mr-3" />
                <span className="text-indigo-800 font-medium">
                  {resumeFile ? resumeFile.name : 'Click to select file (max 5MB)'}
                </span>
              </div>
              {resumeFile && (
                <span className="text-sm text-gray-500">
                  {(resumeFile.size / 1024 / 1024).toFixed(2)} MB
                </span>
              )}
            </div>
          </div>

          {/* Job Description Textarea */}
          <div>
            <label htmlFor="job-description" className="block text-lg font-medium text-gray-700 mb-2">
              2. Enter Job Description
            </label>
            <textarea
              id="job-description"
              rows="8"
              value={jobDescription}
              onChange={(e) => setJobDescription(e.target.value)}
              placeholder="Paste the full job description here, including requirements and responsibilities..."
              className="w-full p-4 border border-gray-300 rounded-xl focus:ring-indigo-500 focus:border-indigo-500 transition resize-none"
              required
            />
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={loading || !resumeFile || !jobDescription.trim()}
            className="w-full flex justify-center items-center py-3 px-4 border border-transparent rounded-xl shadow-md text-lg font-bold text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 transition duration-150 disabled:opacity-50"
          >
            {loading ? (
              <>
                <Loader className="w-5 h-5 mr-2 animate-spin" />
                Analyzing...
              </>
            ) : (
              <>
                <Send className="w-5 h-5 mr-2" />
                Get Suitability Analysis
              </>
            )}
          </button>
        </form>

        {/* Error Message */}
        {error && (
          <div className="mt-6 p-4 bg-red-100 border border-red-400 text-red-700 rounded-xl">
            <p className="font-semibold">Error:</p>
            <p>{error}</p>
          </div>
        )}

        {/* Analysis Results Display */}
        {analysisResult && (
          <AnalysisDisplay result={analysisResult} styles={scoreStyles} />
        )}

        <div className="mt-8 pt-6 border-t text-sm text-gray-400 text-center">
          <p>Powered by Spring Boot (Backend) and Gemini 2.5 Flash (AI Analysis)</p>
        </div>
      </div>
    </div>
  );
};

export default App;