# Search-based reference matcher

This is a JAVA implementation of the Search-Based Matching with Validation (SBMV) algorithm. It can be used to find the target DOI of the document referenced by a given reference (structured or unstructured).

To match a single reference string:

```
org.crossref.refmatching.ReferenceMatcher -it string -i "Jen CK, Foner SN, Cochran EL, Bowers VA (1958) Phys Rev 112:1169"
```

To match references from a file:

```
org.crossref.refmatching.ReferenceMatcher -it file -i /file/path/with/ref/strings/one/per/line -o /output/file/path
```

Output file is in JSON format.

To match multiple structured references:

```
org.crossref.refmatching.ReferenceMatcher -i json -f /json/file/path -o /output/file/path
```

Input JSON file should contain an array of structured references, for example:

```
[  
 {  
  "volume": "39",  
  "year": "1970",  
  "author": "NISHINA K.",  
  "journal-title": "Nucl. Sci. Eng.",  
  "first-page": "170"  
 },  
 {  
  "volume": "40",  
  "year": "2003",  
  "author": "Yang",  
  "journal-title": "J Macromol Sci Pure Appl Chem",  
  "first-page": "309"  
 },  
 {  
  "volume": "16",  
  "year": "2006",  
  "author": "Hatakeyama",  
  "journal-title": "Europ Radiol",  
  "first-page": "2594",  
  "article-title": "Intracranial 2D and 3D DSA with flat panel detector of the direct conversion type: initial experience"  
 }  
]  
```

Output file is also in JSON format.
