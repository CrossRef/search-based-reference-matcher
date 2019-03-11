package org.crossref.refmatching;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author Dominika Tkaczyk
 */
public class UtilsTest {

    @Test
    public void testNormalize() {
        assertEquals("bath", Utils.normalize("Båth"));
        assertEquals("155-9", Utils.normalize("155–9"));
        assertEquals("9* 1 million", Utils.normalize("9· 1 million"));
        assertEquals(
                "psychologische lang unveroffentlichter philipps-universitat.",
                Utils.normalize("Psychologische Läng Unveröffentlichter "
                        + "Philipps-Universität."));
        assertEquals("\"integral invariants of ito' s equations,\"",
                Utils.normalize("“Integral invariants of Itô’ s equations,”"));
        assertEquals(
                "m. v. alfimov, \"photonics of supramolecular nanostructures,\"",
                Utils.normalize("M. V. Alfimov, “Photonics of Supramolecular "
                        + "Nanostructures,”"));
        assertEquals("guezennec", Utils.normalize("Guézennec"));
        assertEquals("5'-amp-activated", Utils.normalize("5′-AMP-activated"));
        assertEquals("kuca", Utils.normalize("Kuča"));
        assertEquals("scharf", Utils.normalize("Schärf"));
        assertEquals("simon", Utils.normalize("Simón"));
        assertEquals("2 years", Utils.normalize("2\u00a0years"));
        assertEquals("pp 201-224", Utils.normalize("pp 201–224"));
        assertEquals("physiological b cell death",
                Utils.normalize("Physiological β cell death"));
        assertEquals("pflugers arch", Utils.normalize("Pflügers Arch"));
        assertEquals("norskov", Utils.normalize("Nørskov"));
        assertEquals("goncalves", Utils.normalize("Gonçalves"));
        assertEquals("ruszczynski", Utils.normalize("Ruszczyński"));
        assertEquals("ronnqvist", Utils.normalize("Rönnqvist"));
        assertEquals(
                "uberblick. in m. prinz (hg.), der lange weg in den uberfluss: "
                + "anfange (s. 437 - 461). paderborn et al.: schoningh",
                Utils.normalize("Überblick. In M. Prinz (Hg.), Der lange Weg in "
                        + "den Überfluss: Anfänge (S. 437 – 461). Paderborn et "
                        + "al.: Schöningh"));
        assertEquals("leseche", Utils.normalize("Lesèche"));
        assertEquals("fur padagogische", Utils.normalize("für Pädagogische"));
        assertEquals("baroja-fernandez e, munoz",
                Utils.normalize("Baroja-Fernández E, Muñoz"));
        assertEquals("dell'acqua tipicita",
                Utils.normalize("Dell’Acqua tipicità"));
    }

    @Test
    public void testStringSimilarity() {
        assertEquals(1., Utils.stringSimilarity("rest Dire Straits",
                                                "Dire Straits", false, true),
                     0.01);
        assertEquals(0.78, Utils.stringSimilarity("rDire Straits est",
                                                  "Straits Dire",
                                                   false, true),
                     0.01);
        assertEquals(0.69, Utils.stringSimilarity("ree Dire Straits",
                                                  "Dear Stratssts", false, true),
                     0.01);
        assertEquals(0.2, Utils.stringSimilarity("Dire Straits", "Slayerwfqw",
                                                 false, true),
                     0.01);

        assertEquals(1., Utils.stringSimilarity("apiDirę Stráits",
                                                "DIre Straîtś", true, true),
                     0.01);
        assertEquals(0.83, Utils.stringSimilarity("Dire StráîtS rest",
                                                  "Straits Dirę", true, true),
                     0.01);
        assertEquals(0.75, Utils.stringSimilarity("DIRe Sträitś",
                                                  "rest DęaR Stráts rest", true,
                                                  true),
                     0.01);
        assertEquals(0.45, Utils.stringSimilarity("Dire Straitśno",
                                                  "rest Sláyer", true, true),
                     0.01);
        
        assertEquals(1., Utils.stringSimilarity("Dire Straits", "Dire Straits",
                                                false, false),
                     0.01);
        assertEquals(0.58, Utils.stringSimilarity("Dire Straits", "Straits Dire",
                                                  false, false),
                     0.01);
        assertEquals(0.78, Utils.stringSimilarity("Dire Straits", "Dear Strats",
                                                  false, false),
                     0.01);
        assertEquals(0.22, Utils.stringSimilarity("Dire Straits", "Slayer",
                                                  false, false),
                     0.01);

        assertEquals(1., Utils.stringSimilarity("Dirę Stráits", "DIre Straîtś",
                                                true, false),
                     0.01);
        assertEquals(0.58, Utils.stringSimilarity("Dire StráîtS", "Straits Dirę",
                                                  true, false),
                     0.01);
        assertEquals(0.78, Utils.stringSimilarity("DIRe Sträitś", "DęaR Stráts",
                                                  true, false),
                     0.01);
        assertEquals(0.22, Utils.stringSimilarity("Dire Straitś", "Sláyer", true,
                                                  false),
                     0.01);
    }

    @Test
    public void testCompleteLastPage() {
        assertEquals("1-8", Utils.completeLastPage("1-8"));
        assertEquals("1-89", Utils.completeLastPage("1-89"));
        assertEquals("10-18", Utils.completeLastPage("10-8"));
        assertEquals("1009-8", Utils.completeLastPage("1009-8"));
        assertEquals("1325-1328", Utils.completeLastPage("1325-8"));
        assertEquals("1325-1328", Utils.completeLastPage("1325-28"));
        assertEquals("1325-128", Utils.completeLastPage("1325-128"));
        assertEquals("1356-1708", Utils.completeLastPage("1356-708"));
        assertEquals("26090-26189", Utils.completeLastPage("26090-189"));
        assertEquals("26090-26099", Utils.completeLastPage("26090-9"));

    }
}