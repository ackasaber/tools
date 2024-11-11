#include "pch.h"
#include "MainWindow.xaml.h"
#if __has_include("MainWindow.g.cpp")
#include "MainWindow.g.cpp"
#endif
#include <Shobjidl.h>
#include <iterator>
#include <microsoft.ui.xaml.window.h>

namespace winrt::AssembleDjvu::implementation
{
    MainWindow::MainWindow()
    {
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
            OutputDebugString(L"Cancelled");
            return;
        }

        winrt::check_hresult(choiceHR);
        OutputDebugString(L"Picked one");
        winrt::com_ptr<IShellItem> choiceItem;
        winrt::check_hresult(dialog->GetResult(choiceItem.put()));
        PWSTR choicePath = nullptr;
        winrt::check_hresult(choiceItem->GetDisplayName(SIGDN_FILESYSPATH, &choicePath));
        MessageBox(NULL, choicePath, L"Chosen", MB_OK);
        CoTaskMemFree(choicePath);
    }

    void MainWindow::ExecuteConvertCommand(XamlUICommand const&, ExecuteRequestedEventArgs const&)
    {
        OutputDebugString(L"TODO ConvertCommand\n");
    }
}
