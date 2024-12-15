#include "pch.h"
#include "MainWindow.xaml.h"
#if __has_include("MainWindow.g.cpp")
#include "MainWindow.g.cpp"
#endif
#include <Shobjidl.h>
#include <iterator>
#include <microsoft.ui.xaml.window.h>
#include "ByteStream.h"
#include "GPixmap.h"
#include "IW44Image.h"
#include "IFFByteStream.h"
#include "DjVuInfo.h"

struct ComStringHolder
{
    ComStringHolder() = default;
    LPWSTR* put() { return &m_string; }
    PWSTR get() { return m_string; }
    ~ComStringHolder() { CoTaskMemFree(m_string); }

private:
    PWSTR m_string = nullptr;
};

struct Win32HandleHolder
{
    Win32HandleHolder(HANDLE handle) :m_handle(handle) { }
    HANDLE get() { return m_handle; }
    ~Win32HandleHolder() { CloseHandle(m_handle); }

private:
    HANDLE m_handle = INVALID_HANDLE_VALUE;
};

namespace winrt::AssembleDjvu::implementation
{
    MainWindow::MainWindow()
    {
    }

    GP<ByteStream> readMemoryMapped(PWSTR filename)
    {
        HANDLE file = CreateFile(filename, GENERIC_READ, FILE_SHARE_READ,
            NULL, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL);

        if (file == INVALID_HANDLE_VALUE)
            winrt::check_win32(GetLastError());

        OutputDebugString(L"Opened the file for shared reading\n");
        Win32HandleHolder fileHandleHolder(file);
        LARGE_INTEGER fileSize{ 0 };
        winrt::check_bool(GetFileSizeEx(file, &fileSize));
        OutputDebugString(L"Received file size\n");

        if (fileSize.QuadPart == 0)
        {
            OutputDebugString(L"Empty file, can't map\n");
            throw winrt::hresult_error();
        }

        HANDLE mapping = CreateFileMapping(file, NULL, PAGE_READONLY, 0, 0, NULL);

        if (mapping == NULL)
            winrt::check_win32(GetLastError());

        OutputDebugString(L"Created the file mapping\n");
        Win32HandleHolder mappingHandleHolder(mapping);
        LPVOID mappedPages = MapViewOfFile(mapping, FILE_MAP_READ, 0, 0, fileSize.QuadPart);
        winrt::check_pointer(mappedPages);
        OutputDebugString(L"Mapped the view\n");

        // copies
        GP<ByteStream> streamHolder = ByteStream::create(mappedPages, fileSize.QuadPart);
        UnmapViewOfFile(mappedPages);
        return streamHolder;
    }

    void writeMemoryMapped(PWSTR filename, GP<ByteStream> buffer)
    {
        HANDLE file = CreateFile(filename, GENERIC_READ | GENERIC_WRITE, 0,
            NULL, CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL);

        if (file == INVALID_HANDLE_VALUE)
            winrt::check_win32(GetLastError());

        OutputDebugString(L"Opened the file for exclusive writing\n");
        Win32HandleHolder fileHandleHolder(file);
        LARGE_INTEGER fileSize;
        fileSize.QuadPart = buffer->size();

        if (fileSize.QuadPart == 0)
        {
            OutputDebugString(L"Empty file, can't map\n");
            throw winrt::hresult_error();
        }

        winrt::check_bool(SetFilePointerEx(file, fileSize, NULL, FILE_BEGIN));
        winrt::check_bool(SetEndOfFile(file));

        HANDLE mapping = CreateFileMapping(file, NULL, PAGE_READWRITE, 0, 0, NULL);

        if (mapping == NULL)
            winrt::check_win32(GetLastError());

        OutputDebugString(L"Created the file mapping\n");
        Win32HandleHolder mappingHandleHolder(mapping);
        LPVOID mappedPages = MapViewOfFile(mapping, FILE_MAP_WRITE, 0, 0, fileSize.QuadPart);
        winrt::check_pointer(mappedPages);
        OutputDebugString(L"Mapped the view\n");

        // copies
        buffer->read(mappedPages, fileSize.QuadPart);
        UnmapViewOfFile(mappedPages);
    }
    
    void MainWindow::ExecuteAddCommand(XamlUICommand const&, ExecuteRequestedEventArgs const&)
    {
        OutputDebugString(L"TODO AddCommand\n");
        auto dialog = winrt::create_instance<IFileOpenDialog>(guid_of<FileOpenDialog>());
        winrt::check_hresult(dialog->SetTitle(L"Choose a JPEG image"));
        COMDLG_FILTERSPEC fileTypes[] = {
            { L"JPEG images", L"*.jpg;*.jpeg"},
            { L"All files", L"*.*" }
        };
        winrt::check_hresult(dialog->SetFileTypes(std::size(fileTypes), fileTypes));
        auto nativeWindow = this->try_as<IWindowNative>();
        HWND parentHwnd{ 0 };
        winrt::check_hresult(nativeWindow->get_WindowHandle(&parentHwnd));
        HRESULT choiceHR = dialog->Show(parentHwnd);

        if (choiceHR == HRESULT_FROM_WIN32(ERROR_CANCELLED)) {
            OutputDebugString(L"Cancelled\n");
            return;
        }

        winrt::check_hresult(choiceHR);
        OutputDebugString(L"Picked one\n");
        winrt::com_ptr<IShellItem> choiceItem;
        winrt::check_hresult(dialog->GetResult(choiceItem.put()));
        ComStringHolder choicePath;
        winrt::check_hresult(choiceItem->GetDisplayName(SIGDN_FILESYSPATH, choicePath.put()));
        MessageBox(NULL, choicePath.get(), L"Chosen", MB_OK);

        GP<ByteStream> streamHolder = readMemoryMapped(choicePath.get());
        ByteStream& stream = *streamHolder;
        GP<GPixmap> pixmapHolder = GPixmap::create(stream);
        GPixmap& pixmap = *pixmapHolder;
        GP<IW44Image> imageHolder = IW44Image::create_encode(pixmap);
        IW44Image& image = *imageHolder;
        GP<ByteStream> outputHolder = ByteStream::create();
        ByteStream& output = *outputHolder;

        GP<IFFByteStream> chunkWriterHolder =
            IFFByteStream::create(outputHolder);
        IFFByteStream& iff = *chunkWriterHolder;

        GP<DjVuInfo> ginfo = DjVuInfo::create();
        DjVuInfo& info = *ginfo;
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
            flag = image.encode_chunk(iff.get_bytestream(), parms[i]);
            iff.close_chunk();
        }
        // Close djvu chunk
        iff.close_chunk();
        output.seek(0);
        writeMemoryMapped(L"C:\\Users\\Admin\\Desktop\\doc.djvu", outputHolder);
    }

    void MainWindow::ExecuteConvertCommand(XamlUICommand const&, ExecuteRequestedEventArgs const&)
    {
        OutputDebugString(L"TODO ConvertCommand\n");
    }
}
