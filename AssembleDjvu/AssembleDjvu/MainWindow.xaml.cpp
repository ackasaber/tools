#include "pch.h"
#include "MainWindow.xaml.h"
#if __has_include("MainWindow.g.cpp")
#include "MainWindow.g.cpp"
#endif

namespace winrt::AssembleDjvu::implementation
{
    MainWindow::MainWindow()
    {
    }

    void MainWindow::ExecuteAddCommand(XamlUICommand const&, ExecuteRequestedEventArgs const&)
    {
        OutputDebugString(L"TODO AddCommand\n");
    }

    void MainWindow::ExecuteConvertCommand(XamlUICommand const&, ExecuteRequestedEventArgs const&)
    {
        OutputDebugString(L"TODO ConvertCommand\n");
    }
}
