cmake_minimum_required(VERSION 4.0)
project(DjvuLibre VERSION 3.5.29)

include(CheckIncludeFile)
check_include_file(cpuid.h HAVE_CPUID_H)
check_include_file(iconv.h HAVE_ICONV)
configure_file(config.h.in config.h)

add_library(djvulibre STATIC
        Arrays.cpp
        Arrays.h
        BSByteStream.cpp
        BSByteStream.h
        BSEncodeByteStream.cpp
        ByteStream.cpp
        ByteStream.h
        CMakeLists.txt
        DataPool.cpp
        DataPool.h
        debug.cpp
        debug.h
        DjVmDir0.cpp
        DjVmDir0.h
        DjVmDir.cpp
        DjVmDir.h
        DjVmDoc.cpp
        DjVmDoc.h
        DjVmNav.cpp
        DjVmNav.h
        DjVuAnno.cpp
        DjVuAnno.h
        DjVuDocEditor.cpp
        DjVuDocEditor.h
        DjVuDocument.cpp
        DjVuDocument.h
        DjVuDumpHelper.cpp
        DjVuDumpHelper.h
        DjVuErrorList.cpp
        DjVuErrorList.h
        DjVuFileCache.cpp
        DjVuFileCache.h
        DjVuFile.cpp
        DjVuFile.h
        DjVuGlobal.cpp
        DjVuGlobal.h
        DjVuGlobalMemory.cpp
        DjVuImage.cpp
        DjVuImage.h
        DjVuInfo.cpp
        DjVuInfo.h
        DjVuMessage.cpp
        DjVuMessage.h
        DjVuMessageLite.cpp
        DjVuMessageLite.h
        DjVuNavDir.cpp
        DjVuNavDir.h
        DjVuPalette.cpp
        DjVuPalette.h
        DjVuPort.cpp
        DjVuPort.h
        DjVuText.cpp
        DjVuText.h
        DjVuToPS.cpp
        DjVuToPS.h
        GBitmap.cpp
        GBitmap.h
        GContainer.cpp
        GContainer.h
        GException.cpp
        GException.h
        GIFFManager.cpp
        GIFFManager.h
        GMapAreas.cpp
        GMapAreas.h
        GOS.cpp
        GOS.h
        GPixmap.cpp
        GPixmap.h
        GRect.cpp
        GRect.h
        GScaler.cpp
        GScaler.h
        GSmartPointer.cpp
        GSmartPointer.h
        GString.cpp
        GString.h
        GThreads.cpp
        GThreads.h
        GUnicode.cpp
        GURL.cpp
        GURL.h
        IFFByteStream.cpp
        IFFByteStream.h
        IW44EncodeCodec.cpp
        IW44Image.cpp
        IW44Image.h
        JB2EncodeCodec.cpp
        JB2Image.cpp
        JB2Image.h
        JPEGDecoder.cpp
        JPEGDecoder.h
        MMRDecoder.cpp
        MMRDecoder.h
        MMX.cpp
        MMX.h
        Template.h
        UnicodeByteStream.cpp
        UnicodeByteStream.h
        XMLParser.cpp
        XMLParser.h
        XMLTags.cpp
        XMLTags.h
        ZPCodec.cpp
        ZPCodec.h)
target_compile_features(djvulibre PRIVATE cxx_std_17)
set_target_properties(djvulibre PROPERTIES CXX_EXTENSIONS OFF)
target_compile_options(djvulibre PRIVATE
        $<$<CXX_COMPILER_ID:MSVC>:/W4 /WX>
        $<$<NOT:$<CXX_COMPILER_ID:MSVC>>:-Wall -Wextra -Wpedantic -Wno-unused-parameter -Werror>)
# config.h
target_include_directories(djvulibre PUBLIC "${CMAKE_CURRENT_BINARY_DIR}" "${CMAKE_CURRENT_LIST_DIR}")

find_package(JPEG)
target_link_libraries(djvulibre PRIVATE JPEG::JPEG)

