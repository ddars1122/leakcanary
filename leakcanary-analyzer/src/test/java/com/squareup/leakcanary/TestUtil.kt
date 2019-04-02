package com.squareup.leakcanary

import java.io.File
import java.lang.ref.PhantomReference
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

internal val NO_EXCLUDED_REFS = ExcludedRefs.builder()
    .build()

internal enum class HeapDumpFile constructor(
  val filename: String,
  val referenceKey: String
) {
  ASYNC_TASK_PRE_M("leak_asynctask_pre_m.hprof", "dc983a12-d029-4003-8890-7dd644c664c5"), //
  ASYNC_TASK_M("leak_asynctask_m.hprof", "25ae1778-7c1d-4ec7-ac50-5cce55424069"), //
  ASYNC_TASK_O("leak_asynctask_o.hprof", "0e8d40d7-8302-4493-93d5-962a4c176089"),
  GC_ROOT_IN_NON_PRIMARY_HEAP(
      "gc_root_in_non_primary_heap.hprof",
      "10a5bc66-e9cb-430c-930a-fc1dc4fc0f85"
  ),
  MISSING_THREAD("missing_thread.hprof", "c644e537-9abd-42e5-994d-032fc150feef")
}

internal fun fileFromName(filename: String): File {
  val classLoader = Thread.currentThread()
      .contextClassLoader
  val url = classLoader.getResource(filename)
  return File(url.path)
}

internal fun findTrackedReferences(heapDumpFile: HeapDumpFile): List<TrackedReference> {
  val file = fileFromName(heapDumpFile.filename)
  val heapAnalyzer = HeapAnalyzer(
      NO_EXCLUDED_REFS, AnalyzerProgressListener.NONE,
      emptyList()
  )
  return heapAnalyzer.findTrackedReferences(file)
}

internal fun analyze(
  heapDumpFile: HeapDumpFile,
  excludedRefs: ExcludedRefs.BuilderWithParams = defaultExcludedRefs
): AnalysisResult {
  val file = fileFromName(heapDumpFile.filename)
  val referenceKey = heapDumpFile.referenceKey
  val heapAnalyzer =
    HeapAnalyzer(excludedRefs.build(), AnalyzerProgressListener.NONE, emptyList());
  val result = heapAnalyzer.checkForLeak(file, referenceKey, true);
  result.failure?.printStackTrace()
  if (result.leakTrace != null) {
    System.out.println(result.leakTrace);
  }
  return result;
}

private val defaultExcludedRefs = ExcludedRefs.BuilderWithParams()
    .clazz(WeakReference::class.java.name)
    .alwaysExclude()
    .clazz(SoftReference::class.java.name)
    .alwaysExclude()
    .clazz(PhantomReference::class.java.name)
    .alwaysExclude()
    .clazz("java.lang.ref.Finalizer")
    .alwaysExclude()
    .clazz("java.lang.ref.FinalizerReference")
    .alwaysExclude()
    .thread("FinalizerWatchdogDaemon")
    .alwaysExclude()
    .thread("main")
    .alwaysExclude()
