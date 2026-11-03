import { Star } from 'lucide-react'
import { TEXTO_ESTRELLAS } from '../formato'

/** Puntuación de solo lectura: cinco estrellas, llenas hasta la puntuación. */
export function Estrellas({ valor, tamano = 16 }) {
  return (
    <span className="estrellas" role="img" aria-label={`${valor} de 5 estrellas`}>
      {[1, 2, 3, 4, 5].map((n) => (
        <Star key={n} size={tamano} aria-hidden="true" className={n <= Math.round(valor) ? 'llena' : ''} />
      ))}
    </span>
  )
}

/**
 * RF075: elegir de 1 a 5 estrellas. Son botones de radio de verdad (dentro de un fieldset), así que
 * con teclado se recorren con las flechas y el lector de pantalla anuncia "Bueno, 4 de 5".
 */
export function SelectorEstrellas({ valor, alCambiar, nombre }) {
  return (
    <fieldset className="selector-estrellas">
      <legend>¿Cómo te fue?</legend>
      <div className="opciones-estrellas">
        {[1, 2, 3, 4, 5].map((n) => (
          <label key={n} className={n <= valor ? 'llena' : ''} title={TEXTO_ESTRELLAS[n]}>
            <input type="radio" name={nombre} value={n} checked={valor === n} onChange={() => alCambiar(n)}
              aria-label={`${TEXTO_ESTRELLAS[n]}, ${n} de 5`} />
            <Star size={30} aria-hidden="true" />
          </label>
        ))}
        <span className="texto-estrellas" aria-hidden="true">{valor ? TEXTO_ESTRELLAS[valor] : 'Elige de 1 a 5'}</span>
      </div>
    </fieldset>
  )
}
