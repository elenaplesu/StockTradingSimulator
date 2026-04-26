export default function NotificationContainer({ notifications, removeNotification }) {
    if (notifications.length === 0) return null;

    return (
        <div style={{
            position: 'fixed',
            bottom: '20px',
            right: '20px',
            zIndex: 9999,
            display: 'flex',
            flexDirection: 'column',
            gap: '10px',
            alignItems: 'flex-end',
            width: '350px'
        }}>
            <style jsx="true">{`
                @keyframes slideIn {
                    from { transform: translateX(100%); opacity: 0; }
                    to { transform: translateX(0); opacity: 1; }
                }
            `}</style>

            {notifications.map((notification) => (
                <div
                    key={notification.id}
                    className={`alert ${notification.isError ? 'alert-danger' : 'alert-success'} shadow-lg alert-dismissible fade show m-0`}
                    style={{
                        width: '350px',
                        minHeight: '80px',
                        maxHeight: '80px',
                        borderLeft: `5px solid ${notification.isError ? '#dc3545' : '#198754'}`,
                        animation: 'slideIn 0.3s ease-out',
                        padding: '12px 35px 12px 15px',
                        display: 'flex',
                        flexDirection: 'column',
                        justifyContent: 'center',
                        overflow: 'hidden'
                    }}
                    role="alert"
                >
                    <div className="fw-bold mb-1" style={{ fontSize: '14px' }}>
                        {notification.isError ? 'Action Failed' : 'Action Successful'}
                    </div>
                    <div style={{ fontSize: '12px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {notification.message}
                    </div>
                    <button
                        type="button"
                        className="btn-close"
                        style={{ position: 'absolute', right: '10px', top: '10px' }}
                        onClick={() => removeNotification(notification.id)}
                    ></button>
                </div>
            ))}
        </div>
    );
}