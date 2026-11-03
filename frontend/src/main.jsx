import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import '@fontsource-variable/plus-jakarta-sans'
import './index.css'
import App from './App.jsx'
import { aplicarApariencia, leerApariencia } from './apariencia'

// Antes del primer render, para que no parpadee el tema equivocado.
aplicarApariencia(leerApariencia())

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
