import { useState } from 'react';
import { API_BASE_URL } from './config';

const checkMatch = (option, answer) => {
    if (!option || !answer) return false;
    const optClean = String(option).trim().toLowerCase();
    const ansClean = String(answer).trim().toLowerCase();
    return optClean === ansClean || ansClean.includes(optClean);
};

// Compact Definitions from Wikipedia
const WIKI_TERMS = [
    { term: "P/E Ratio", desc: "Measures a company's current share price relative to its per-share earnings." },
    { term: "ROI", desc: "Return on investment is a ratio between net income and investment to evaluate profitability." },
    { term: "Diversification", desc: "Allocating capital in a way that reduces exposure to any one particular asset or risk." },
    { term: "ETF", desc: "An exchange-traded fund is a basket of securities that trades on an exchange like a stock." }
];

export default function Learn({ userId }) {
    const [quizStarted, setQuizStarted] = useState(false);
    const [quizFinished, setQuizFinished] = useState(false);
    const [questions, setQuestions] = useState([]);
    const [currentIdx, setCurrentIdx] = useState(0);
    const [score, setScore] = useState(0);
    const [isGenerating, setIsGenerating] = useState(false);
    const [selectedAnswer, setSelectedAnswer] = useState(null);
    const [isAnswered, setIsAnswered] = useState(false);

    const generateQuiz = () => {
        setIsGenerating(true);

        let endpoint = `${API_BASE_URL}/api/learn/general-quiz`;
        if (userId) {
            endpoint = `${API_BASE_URL}/api/learn/ai-quiz/${userId}`;
        }

        fetch(endpoint)
            .then(res => {
                if (!res.ok) throw new Error("API Failed");
                return res.json();
            })
            .then(quizData => {
                // Shuffle options
                const dataArray = Array.isArray(quizData) ? quizData : quizData.questions || [];
                const finalQuiz = dataArray.map(q => ({
                    ...q,
                    options: [...q.options].sort(() => Math.random() - 0.5)
                }));

                setQuestions(finalQuiz);
                setScore(0);
                setCurrentIdx(0);
                setQuizStarted(true);
                setQuizFinished(false);
                setSelectedAnswer(null);
                setIsAnswered(false);
                setIsGenerating(false);
            })
            .catch(err => {
                console.error("Quiz Fetch Error:", err);
                setIsGenerating(false);
            });
    };

    const handleAnswer = (selectedOption) => {
        if (isAnswered) return;
        setSelectedAnswer(selectedOption);
        setIsAnswered(true);

        if (checkMatch(selectedOption, questions[currentIdx].answer)) {
            setScore(score + 1);
        }
    };

    const handleNext = () => {
        if (currentIdx + 1 < questions.length) {
            setCurrentIdx(currentIdx + 1);
            setSelectedAnswer(null);
            setIsAnswered(false);
        } else {
            setQuizFinished(true);
        }
    };

    const restartQuiz = () => {
        setQuizStarted(false);
        setQuizFinished(false);
    };


    const Footer = () => (
        <footer className="py-2 text-center border-top mt-auto bg-light" style={{ fontSize: '0.75rem' }}>
            <span className="text-muted">
                Definitions sourced from <strong>Wikipedia</strong>. Quiz data generated dynamically via <strong>Groq Llama 3.1</strong>.
            </span>
        </footer>
    );

    // VIEW 1: START SCREEN (Definitions + Start Button)
    if (!quizStarted) {
        return (
            <div className="d-flex flex-column vh-100 overflow-hidden bg-light">
                <div className="flex-grow-1 d-flex flex-column align-items-center justify-content-center p-3">
                    <h2 className="fw-bold mb-1">{userId ? "Your Custom Trading Quiz" : "Test Your Market Knowledge"}</h2>
                    <p className="text-muted small mb-4">
                        {userId
                            ? "Questions generated dynamically based on your live portfolio holdings."
                            : "A fast-paced quiz covering essential stock market and investing concepts."}
                    </p>

                    //Compact Study Guide Grid
                    <div className="row w-100 justify-content-center mb-4" style={{ maxWidth: '800px' }}>
                        {WIKI_TERMS.map((item, idx) => (
                            <div key={idx} className="col-md-6 col-12 mb-2">
                                <div className="card shadow-sm border-0 h-100 p-2 text-start">
                                    <span className="badge bg-primary text-white w-25 mb-1" style={{ fontSize: '0.7rem' }}>{item.term}</span>
                                    <span className="text-muted" style={{ fontSize: '0.8rem', lineHeight: '1.2' }}>{item.desc}</span>
                                </div>
                            </div>
                        ))}
                    </div>

                    <button
                        className="btn btn-primary shadow-sm px-5 py-2 fw-bold"
                        onClick={generateQuiz}
                        disabled={isGenerating}
                    >
                        {isGenerating ? (
                            <><span className="spinner-border spinner-border-sm me-2"></span> Preparing quiz...</>
                        ) : (
                            userId ? "Analyze Portfolio & Start AI Quiz" : "Start General Market Quiz"
                        )}
                    </button>
                </div>
                <Footer />
            </div>
        );
    }

    //  VIEW 2: QUIZ FINISHED
    if (quizFinished) {
        const passColor = score >= questions.length / 2 ? '#198754' : '#dc3545';
        return (
            <div className="d-flex flex-column vh-100 overflow-hidden bg-light">
                <div className="flex-grow-1 d-flex align-items-center justify-content-center">
                    <div className="card shadow-sm border-0 p-5 text-center" style={{ width: '400px' }}>
                        <h3 className="fw-bold mb-2">Quiz Complete! 🎉</h3>
                        <p className="text-muted mb-4">You have successfully completed this module.</p>
                        <div className="display-4 fw-bold mb-4" style={{ color: passColor }}>
                            {score} / {questions.length}
                        </div>
                        <button className="btn btn-primary w-100" onClick={restartQuiz}>
                            Return to Study Guide
                        </button>
                    </div>
                </div>
                <Footer />
            </div>
        );
    }

    //  VIEW 3: ACTIVE QUIZ
    const currentQ = questions[currentIdx];

    return (
        <div className="d-flex flex-column vh-100 overflow-hidden bg-light">
            <div className="flex-grow-1 d-flex flex-column align-items-center justify-content-center p-3">
                <div style={{ maxWidth: '650px', width: '100%' }}>
                    <div className="d-flex justify-content-between align-items-center mb-2 px-2">
                        <span className="fw-bold text-muted" style={{ fontSize: '0.85rem' }}>Question {currentIdx + 1} of {questions.length}</span>
                        <span className="fw-bold text-primary" style={{ fontSize: '0.85rem' }}>Score: {score}</span>
                    </div>

                    <div className="card shadow-sm border-0 p-4" style={{ minHeight: '400px' }}>
                        <h5 className="mb-4 text-center lh-base fw-bold">{currentQ?.q}</h5>

                        <div className="d-flex flex-column gap-2 mb-auto">
                            {currentQ?.options.map((opt, idx) => {
                                let btnClass = "btn btn-outline-dark text-start py-2 px-3 transition-all";

                                if (isAnswered) {
                                    const isThisTheCorrectAnswer = checkMatch(opt, currentQ.answer);
                                    const didUserSelectThis = selectedAnswer === opt;

                                    if (isThisTheCorrectAnswer) {
                                        btnClass = "btn btn-success text-start py-2 px-3 shadow-sm text-white";
                                    } else if (didUserSelectThis && !isThisTheCorrectAnswer) {
                                        btnClass = "btn btn-danger text-start py-2 px-3 opacity-75 text-white";
                                    } else {
                                        btnClass = "btn btn-outline-secondary text-start py-2 px-3 opacity-50";
                                    }
                                }

                                return (
                                    <button
                                        key={idx}
                                        className={btnClass}
                                        onClick={() => handleAnswer(opt)}
                                        disabled={isAnswered}
                                        style={{ fontSize: '0.95rem' }}
                                    >
                                        <span className="fw-bold me-3">{String.fromCharCode(65 + idx)}.</span> {opt}
                                    </button>
                                );
                            })}
                        </div>

                        <div
                            className="mt-4 text-end border-top pt-3"
                            style={{ visibility: isAnswered ? 'visible' : 'hidden' }}
                        >
                            <button className="btn btn-primary px-4 shadow-sm fw-bold" onClick={handleNext}>
                                {currentIdx + 1 === questions.length ? "Finish Quiz" : "Next Question ➔"}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
            <Footer />
        </div>
    );
}