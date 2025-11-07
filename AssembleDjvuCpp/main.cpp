#include "GURL.h"
#include "ByteStream.h"
#include "GPixmap.h"
#include "IW44Image.h"
#include "IFFByteStream.h"
#include "DjVuInfo.h"
#include "DjVuDocEditor.h"

#include <filesystem>
#include <vector>
#include <print>
#include <string_view>

using namespace std::string_view_literals;
using namespace DJVU;

static auto inputDir = "/home/alex/Загрузки/assembly";

static void ConvertPage(const GURL& source, const GURL& dest) {
    GP<ByteStream> jpegHolder = ByteStream::create(source, "rb");
    ByteStream& jpeg = *jpegHolder;
    GP<GPixmap> pixmapHolder = GPixmap::create(jpeg);
    GPixmap& pixmap = *pixmapHolder;
    GP<IW44Image> iw44Holder = IW44Image::create_encode(pixmap);
    IW44Image& iw44 = *iw44Holder;
    GP<IFFByteStream> iffHolder = IFFByteStream::create
        (ByteStream::create(dest, "wb"));
    IFFByteStream& iff = *iffHolder;

    GP<DjVuInfo> infoHolder = DjVuInfo::create();
    DjVuInfo& info = *infoHolder;
    info.width = pixmap.columns();
    info.height = pixmap.rows();
    info.dpi = 100;
    info.gamma = 2.2;
    // Write djvu header and info chunk
    iff.put_chunk("FORM:DJVU", 1);
    iff.put_chunk("INFO");
    info.encode(*iff.get_bytestream());
    iff.close_chunk();
    // Write all chunks
    int flag = 1;
    constexpr int nchunks = 3;
    IWEncoderParms parms[nchunks];
    int argv_slice[] = { 74, 89, 99 };
    for (int i = 0; i < nchunks; i++)
    {
        parms[i].bytes = 0;
        parms[i].slices = argv_slice[i];
        parms[i].decibels = 0;
    }
    for (int i = 0; flag && i < nchunks; i++)
    {
        iff.put_chunk("BG44");
        flag = iw44.encode_chunk(iff.get_bytestream(), parms[i]);
        iff.close_chunk();
    }
    // Close djvu chunk
    iff.close_chunk();
}

int main() {
    const auto imageSuffix = ".jpg"sv;
    std::filesystem::path inputDirPath = inputDir;
    std::vector<std::filesystem::path> inputFiles;

    for (const auto& entry : std::filesystem::directory_iterator(inputDirPath)) {
        if (entry.is_regular_file() && entry.path().filename().string().ends_with(imageSuffix)) {
            inputFiles.push_back(entry.path());
        }
    }

    GList<GURL> pageList;

    for (const auto& imagePath : inputFiles) {
        auto filename = imagePath.filename().string();
        auto n = filename.length();
        auto convFilename = filename;
        convFilename.replace(n - imageSuffix.length(), imageSuffix.length(), ".djvu");
        auto convPath = imagePath;
        convPath.replace_filename(convFilename);
        std::println("{} -> {}", imagePath.string(), convPath.string());

        auto src = GURL::Filename::UTF8(imagePath.c_str());
        auto dst = GURL::Filename::UTF8(convPath.c_str());
        ConvertPage(src, dst);
        pageList.append(dst);
    }

    GP<DjVuDocEditor> doc=DjVuDocEditor::create_wait();
    doc->insert_group(pageList);
    auto bundleName = inputDirPath / "doc.djvu";
    auto bundleUrl = GURL::Filename::UTF8(bundleName.c_str());
    doc->save_as(bundleUrl, true);

    return 0;
}
