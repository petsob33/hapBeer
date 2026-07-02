package cz.sobtech.hapBeer.ui.util

object FunnyMessages {

    val PO_PIVO = listOf(
        "Do dna!",
        "Ať ti chutná!",
        "Další jedno pro statistiku.",
        "Pivo teče proudem!",
        "Zlatavý mok doručen.",
        "Správná volba!",
        "Zdraví pivovaru!",
        "Jedeš!",
        "Tahle práce se musí dělat.",
        "Tak zase jednou.",
        "Žízeň poražena!",
        "Pivař sobě.",
        "Šupem do sebe!",
        "Krůček k legendě.",
        "Pivní karma +1."
    )

    val MILNIK = listOf(
        "{jméno} právě pokořil(a) {počet} piv! Legenda!",
        "Pozor, {jméno} se blíží rekordu! {počet} piv!",
        "{jméno} je na {počet} – kdo ho/ji zastaví?",
        "{počet} piv pro {jméno}! Tleská celá hospoda!",
        "{jméno} nasbíral(a) {počet} piv. Jde to!",
        "Míle {počet} pokořena! {jméno} nezastavitelný/á!",
        "{jméno}: {počet} piv. To je výkon!",
        "Gratulace! {jméno} právě dosáhl(a) {počet} piv.",
        "{počet} piv – a {jméno} pořád vypadá dobře!",
        "Rekordní výkon: {jméno} s {počet} pivy!"
    )

    val BECKA_DOCHAZI = listOf(
        "Bečka je na kolenou, doplňte zásoby!",
        "SOS! Málo piva!",
        "Výstraha: bečka se blíží ke dnu!",
        "Zásoby kriticky nízké, volejte výzbroj!",
        "Bečka volá o pomoc – objednejte záchranu!"
    )

    val BECKA_PRAZDNA = listOf(
        "Konec, bečka je v pivním nebi.",
        "Prázdno jak v peněžence po výplatě.",
        "Bečka odešla do věčných lovišť.",
        "Tady se pít nedá. Sehnat další!",
        "Sucho totální. Bečka je fuč."
    )

    val PRVNI_MISTO = listOf(
        "Pivní král/královna večera!",
        "Nedostižný/á na trůnu!",
        "MVP pivního dne!",
        "Legenda mezi námi!",
        "Nezastavitelný(á) šampion(ka)!"
    )

    fun pickRandom(list: List<String>): String = list.random()

    fun milnikMessage(jmeno: String, pocet: Int): String =
        pickRandom(MILNIK)
            .replace("{jméno}", jmeno)
            .replace("{počet}", pocet.toString())
}
