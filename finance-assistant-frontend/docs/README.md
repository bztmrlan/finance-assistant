# Finance Assistant Frontend

A modern, responsive authorization page built with vanilla HTML, CSS, and JavaScript that integrates with the Finance Assistant Backend API.

## Features

- **Modern UI/UX**: Clean, responsive design with smooth animations
- **Tab-based Interface**: Easy switching between login and registration forms
- **Form Validation**: Real-time client-side validation with visual feedback
- **JWT Authentication**: Secure token-based authentication
- **Password Visibility Toggle**: Show/hide password functionality
- **Loading States**: Visual feedback during API calls
- **Error Handling**: User-friendly error messages
- **Responsive Design**: Works on all device sizes
- **No Framework Dependencies**: Pure vanilla JavaScript implementation

## File Structure

```
finance-assistant-frontend/
├── index.html          # Main authorization page
├── dashboard.html      # Dashboard page (after authentication)
├── styles.css          # CSS styling
├── config.js           # Configuration file (ports, API settings)
├── auth.js            # Authentication logic and API integration
├── test.html          # Testing page for API connectivity
├── test-config.html   # Configuration verification page
├── start.sh           # Startup script for local development
└── README.md          # This file
```

## API Integration

The frontend integrates with the following backend endpoints from your `AuthController`:

### Authentication Endpoints

| Endpoint | Method | Purpose | Request Body | Response |
|----------|--------|---------|--------------|----------|
| `/auth/login` | POST | User login | `{email, password}` | `{token}` |
| `/auth/register` | POST | User registration | `{name, email, password}` | `{token}` |
| `/auth/health` | GET | Health check | None | Status message |

### Request/Response Format

#### Login Request
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

#### Login Response
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Register Request
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123"
}
```

#### Register Response
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

## Setup Instructions

### 1. Backend Configuration

Ensure your Spring Boot backend is running on `http://localhost:8080` (or update the `API_BASE_URL` in `auth.js`).

### 2. CORS Configuration

Your backend needs to allow CORS requests from the frontend. Add this configuration to your Spring Boot application:

```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000", "http://127.0.0.1:3000", "file://")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
```

### 3. Frontend Setup

1. **Local Development**: Open `index.html` in a web browser
2. **HTTP Server**: Use a local HTTP server to avoid CORS issues:
   ```bash
   # Using Python 3
   python -m http.server 3000
   
   # Using Node.js (if you have http-server installed)
   npx http-server -p 3000
   
   # Using PHP
   php -S localhost:3000
   ```

3. **Access the application**: Open `http://localhost:3000` in your browser

## Usage

### Authentication Flow

1. **Login**: Enter email and password, click "Sign In"
2. **Registration**: Fill in name, email, password, and confirm password, click "Create Account"
3. **Success**: JWT token is stored in localStorage and user is redirected to dashboard
4. **Dashboard**: Protected page that shows user information and main features

### JWT Token Management

- **Storage**: Tokens are stored in `localStorage`
- **Validation**: Frontend checks token validity on page load
- **Logout**: Tokens are removed from localStorage on logout

### Form Validation

- **Email**: Must be a valid email format
- **Password**: Minimum 6 characters
- **Name**: Required for registration
- **Password Confirmation**: Must match password for registration

## Security Features

- **Password Hashing**: Backend handles password encryption
- **JWT Tokens**: Secure, stateless authentication
- **Input Validation**: Client and server-side validation
- **HTTPS Ready**: Configure for production use

## Customization

### Styling
- Modify `styles.css` to change colors, fonts, and layout
- Update CSS variables for consistent theming
- Adjust responsive breakpoints as needed

### Functionality
- Extend `auth.js` to add more features
- Modify form validation rules
- Add additional API endpoints

### API Configuration
- Update `config.js` for different environments and ports
- All API settings are centralized in the configuration file
- Modify request/response handling as needed

## Browser Compatibility

- **Modern Browsers**: Chrome 60+, Firefox 55+, Safari 12+, Edge 79+
- **Features Used**: ES6+, Fetch API, CSS Grid, Flexbox
- **Fallbacks**: Basic functionality works in older browsers

## Troubleshooting

### Common Issues

1. **CORS Errors**: Ensure backend CORS configuration is correct
2. **API Connection**: Check if backend is running on correct port
3. **JWT Issues**: Verify JWT secret and expiration settings in backend
4. **Form Validation**: Check browser console for JavaScript errors

### Debug Mode

Enable debug logging by adding this to browser console:
```javascript
localStorage.setItem('debug', 'true');
```

## Production Deployment

1. **HTTPS**: Use HTTPS in production for security
2. **Environment Variables**: Configure API URLs for production
3. **Error Handling**: Implement proper error logging
4. **Performance**: Minify CSS/JS files
5. **Security Headers**: Add security headers to your web server

## Contributing

1. Follow the existing code style
2. Test changes across different browsers
3. Ensure responsive design works on all screen sizes
4. Update documentation for any new features

## License

This frontend is part of the Finance Assistant project and follows the same license terms. 