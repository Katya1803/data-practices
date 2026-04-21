import { BrowserRouter, Routes, Route, NavLink } from 'react-router-dom'
import Dashboard from './pages/Dashboard'
import Films from './pages/Films'
import Customers from './pages/Customers'
import Rentals from './pages/Rentals'
import Payments from './pages/Payments'
import Reports from './pages/Reports'
import Activity from './pages/Activity'

export default function App() {
  return (
    <BrowserRouter>
      <nav>
        <span>DVDRental</span>
        <NavLink to="/" end>Dashboard</NavLink>
        <NavLink to="/films">Films</NavLink>
        <NavLink to="/customers">Customers</NavLink>
        <NavLink to="/rentals">Rentals</NavLink>
        <NavLink to="/payments">Payments</NavLink>
        <NavLink to="/reports">Reports</NavLink>
        <NavLink to="/activity">Activity</NavLink>
      </nav>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/films" element={<Films />} />
        <Route path="/customers" element={<Customers />} />
        <Route path="/rentals" element={<Rentals />} />
        <Route path="/payments" element={<Payments />} />
        <Route path="/reports" element={<Reports />} />
        <Route path="/activity" element={<Activity />} />
      </Routes>
    </BrowserRouter>
  )
}
