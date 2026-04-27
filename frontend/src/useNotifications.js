import {useState, useCallback, useRef} from 'react';

export function useNotifications() {
    const [notifications, setNotifications] = useState([]);
    const idCounter = useRef(0);
    const addNotification = useCallback((message, isError) => {
        const id = ++idCounter.current;
        setNotifications(prev => [...prev, { id, message, isError }]);

        setTimeout(() => {
            setNotifications(prev => prev.filter(n => n.id !== id));
        }, 5000);
    }, []);

    const removeNotification = useCallback((id) => {
        setNotifications(prev => prev.filter(n => n.id !== id));
    }, []);

    return { notifications, addNotification, removeNotification };
}