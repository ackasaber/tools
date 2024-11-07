#include "pch.h"
#include "MainWindow.xaml.h"
#if __has_include("MainWindow.g.cpp")
#include "MainWindow.g.cpp"
#endif

using namespace winrt;
using namespace Microsoft::UI::Xaml;
using namespace Microsoft::UI::Xaml::Input;
using namespace Windows::Foundation;

// To learn more about WinUI, the WinUI project structure,
// and more about our project templates, see: http://aka.ms/winui-project-info.

namespace winrt::AssembleDjvu::implementation
{
    MainWindow::MainWindow()
    {
    }

    void MainWindow::ExecuteAddCommand(XamlUICommand const&, ExecuteRequestedEventArgs const&)
    {
        OutputDebugString(L"Clicked\n");
    }
}
