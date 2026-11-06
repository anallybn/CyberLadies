const API_URL = import.meta.env.VITE_API_URL







function cabecalhos() {
    const token = localStorage.getItem('token')
    return {
        'Content-Type': 'application/json',
        ...API_URL(token ?{ Authorization})
    }
}