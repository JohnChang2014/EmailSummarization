import gate.termraider.*;

// equivalent to old preprocessor
eachDocument {
    docReset();
    languageIdentifier();
    tokenizer();
    gazetteer();
    bwpGazetteer();
    senSpliter();
    posTagger();
    morphAnalyzer();
    stopWordMarker();
    simpleNounChunker();
    neExtracter();
    vpChunker();
    orthoMatcher();
    selectTokens();
    multiwordJape();
    deduplicateMW();
}

// create the TfIdfTermbank LR
def termbank0 = Factory.createResource("gate.termraider.bank.TfIdfTermbank", [
  idfCalculation: 'LogarithmicPlus1',
  name: 'tfIdfTermbank',
  corpora:[corpus],
  inputAnnotationTypes:'SingleWord;MultiWord;VG;Person;NounChunk;Date;Location;TermCandidate',
  debugMode:true,
].toFeatureMap())

eachDocument {
    tfIdfCopier(termbank:termbank0);
}
// To access the result of termbanks, we need to store the reference of termbanks
// into feature map of the corpus, so in Java just use
// "TfIdfTermbank myBank = (TfIdfTermbank) controller.getCorpus().getFeatures().get("tfidfTermbank");"
// to access data inside the termbank.
corpus.features.tfidfTermbank = termbank0