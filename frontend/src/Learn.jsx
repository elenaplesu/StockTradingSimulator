import { useState } from 'react';

export default function Learn({ userId }) {
    const [quizStarted, setQuizStarted] = useState(false);
    const [questions, setQuestions] = useState([]);
    const [currentIdx, setCurrentIdx] = useState(0);
    const [score, setScore] = useState(0);
    const [isGenerating, setIsGenerating] = useState(false);
    const [error, setError] = useState(null);

    const generateCustomQuiz = () => {
        if (!userId) {
            setError("Please log in to generate a custom quiz.");
            return;
        }

        setIsGenerating(true);
        setError(null);

        // Fetch the quiz directly from your new AI Spring Boot endpoint
        fetch(`http://localhost:8080/api/learn/ai-quiz/${userId}`)
            .then(res => {
                if (!res.ok) throw new Error("Failed to generate quiz");
                return res.json();
            })
            .then(aiQuizData => {
                // Randomize options so the correct answer isn't always in the same spot
                const shuffledQuestions = aiQuizData.map(question => ({
                    ...question,
                    options: question.options.sort(() => Math.random() - 0.5)
                }));

                setQuestions(shuffledQuestions);
                setScore(0);
                setCurrentIdx(0);
                setQuizStarted(true);
                setIsGenerating(false);
            })
            .catch(err => {
                console.error("AI Quiz Error:", err);
                setError("Failed to connect to the Google Gemini AI. Please try again.");
                setIsGenerating(false);
            });
    };

    const handleAnswer = (selectedOption) => {
        const isCorrect = selectedOption === questions[currentIdx].answer;
        if (isCorrect) {
            setScore(score + 1);
        }

        if (currentIdx + 1 < questions.length) {
            setCurrentIdx(currentIdx + 1);
        } else {
            // End of Quiz
            alert(`Quiz Complete! You scored ${isCorrect ? score + 1 : score} / ${questions.length}`);
            setQuizStarted(false);
        }
    };

    // UI SCREEN 1: The Start Button
    if (!quizStarted) {
        return (
            <div className="container mt-5 text-center">
                <h2 className="fw-bold">Your Custom Trading Quiz</h2>
                <p className="text-muted">Questions generated dynamically by Google Gemini based on your live portfolio.</p>

                {error && <div className="alert alert-danger w-50 mx-auto">{error}</div>}

                <button
                    className="btn btn-primary btn-lg mt-3 shadow-sm"
                    onClick={generateCustomQuiz}
                    disabled={isGenerating}
                >
                    {isGenerating ? (
                        <><span className="spinner-border spinner-border-sm me-2"></span> AI is analyzing your portfolio...</>
                    ) : (
                        "Analyze Portfolio & Start AI Quiz"
                    )}
                </button>
            </div>
        );
    }

    // UI SCREEN 2: The Active Quiz
    const currentQ = questions[currentIdx];

    return (
        <div className="container mt-5 mb-5 w-75 mx-auto">
            <h5 className="text-muted text-center mb-4">Question {currentIdx + 1} of {questions.length}</h5>
            <div className="card shadow border-0 p-5">
                <h3 className="mb-4 text-center lh-base">{currentQ.q}</h3>
                <div className="d-flex flex-column gap-3 mt-4">
                    {currentQ.options.map((opt, idx) => (
                        <button
                            key={idx}
                            className="btn btn-outline-dark btn-lg text-start py-3"
                            onClick={() => handleAnswer(opt)}
                        >
                            <span className="fw-bold me-3">{String.fromCharCode(65 + idx)}.</span> {opt}
                        </button>
                    ))}
                </div>
            </div>
        </div>
    );
}