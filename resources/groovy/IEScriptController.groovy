import gate.termraider.*;

// equivalent to old preprocessor
eachDocument {
    docReset();
    languageIdentifier();
    tokenizer();
    //gazetteer();
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
  inputAnnotationTypes:'SingleWord;MultiWord;VG;Person;NounChunk;Date;Money',
  debugMode:true,
].toFeatureMap())

//tfIdfCopier.termbank = termbank0;

eachDocument {
    tfIdfCopier(termbank:termbank0);
    //augmentation();
}

/*
// create the AugmentedTermbank LR
def termbank1 = Factory.createResource("gate.termraider.bank.AnnotationTermbank", [
  corpora:[corpus],
  inputAnnotationTypes:'SingleWord;MultiWord;VG;Person;NounChunk;Date;Money',
  debugMode:true,
  inputScoreFeature:'localAugTfIdf'
].toFeatureMap())

// create the HyponymyTermbank LR
def termbank2 = Factory.createResource("gate.termraider.bank.HyponymyTermbank", [
  corpora:[corpus],
  inputAnnotationTypes:'SingleWord;MultiWord;VG;Person;NounChunk;Date;Money',
  debugMode:true,
  inputHeadFeatures:'head;canonical',
  inputAnnotationFeature:'canonical'
].toFeatureMap())

eachDocument {
    augTfIdfCopier(termbank:termbank1);
    kyotoCopier(termbank:termbank2);
}
*/
// To access the result of termbanks, we need to store the reference of termbanks
// into feature map of the corpus, so in Java just use
// "TfIdfTermbank myBank = (TfIdfTermbank) controller.getCorpus().getFeatures().get("tfidfTermbank");"
// to access data inside the termbank.
corpus.features.tfidfTermbank = termbank0
//corpus.features.annotationTermbank = termbank1
//corpus.features.hyponymyTermbank = termbank2