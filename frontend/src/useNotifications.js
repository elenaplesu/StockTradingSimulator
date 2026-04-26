import { useState, useCallback } from 'react';

export function useNotifications() {
    const [notifications, setNotifications] = useState([]);

    const addNotification = useCallback((message, isError) => {
        const id = Date.now();
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