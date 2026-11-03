// Sierra Nevada sobre el mar Caribe, como el logo de los mockups.
export default function Logo({ tamano = 40 }) {
  return (
    <svg className="logo-svg" width={tamano} height={tamano} viewBox="0 0 48 48" aria-hidden="true">
      <path d="M3 33 L16 12 L22.5 22 L29 14 L45 33 Z" fill="currentColor" />
      <path d="M16 12 L20 18.3 L17.6 17.2 L16 19 L14.3 17.2 L12.3 18.1 Z" fill="var(--superficie)" opacity="0.9" />
      <path d="M3 38.5 q5.25 -4 10.5 0 t10.5 0 t10.5 0 t10.5 0" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" />
      <path d="M8 44 q4 -3 8 0 t8 0 t8 0 t8 0" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" opacity="0.6" />
    </svg>
  )
}
